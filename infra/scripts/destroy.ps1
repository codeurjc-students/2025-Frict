$ErrorActionPreference = "Continue"

$REGION = "eu-west-1"
$STACK_NAME = "frict"

Write-Host "===============================" -ForegroundColor Yellow
Write-Host "  Destroying environment" -ForegroundColor Yellow
Write-Host "===============================" -ForegroundColor Yellow
Write-Host ""

# ── Get account ID ──
$ACCOUNT_ID = aws sts get-caller-identity --query Account --output text --region $REGION
Write-Host "Account: $ACCOUNT_ID"

# ── Find VPC before deleting stack ──
$VPC_ID = "None"
try {
    $NETWORK_STACK = aws cloudformation list-stack-resources `
        --stack-name $STACK_NAME `
        --query "StackResourceSummaries[?LogicalResourceId=='NetworkStack'].PhysicalResourceId | [0]" `
        --output text --region $REGION 2>$null
    if ($NETWORK_STACK -and $NETWORK_STACK -ne "None") {
        $VPC_ID = aws cloudformation describe-stacks `
            --stack-name $NETWORK_STACK `
            --query "Stacks[0].Outputs[?OutputKey=='VPCId'].OutputValue | [0]" `
            --output text --region $REGION 2>$null
    }
} catch {}

if (-not $VPC_ID -or $VPC_ID -eq "None") {
    $VPC_ID = aws ec2 describe-vpcs `
        --filters "Name=isDefault,Values=false" `
        --query "Vpcs[?Tags[?contains(Value,'$STACK_NAME')]].VpcId | [0]" `
        --output text --region $REGION 2>$null
}

Write-Host "VPC:     $VPC_ID"
Write-Host ""

# ── Delete CloudFormation stack ──
Write-Host "[1/7] Deleting CloudFormation stack..." -ForegroundColor Cyan
$STATUS = aws cloudformation describe-stacks `
    --stack-name $STACK_NAME `
    --query "Stacks[0].StackStatus" --output text `
    --region $REGION 2>$null

if (-not $STATUS) {
    Write-Host "  Stack does not exist, skipping"
} else {
    Write-Host "  Status: $STATUS"
    aws cloudformation delete-stack --stack-name $STACK_NAME --region $REGION

    Write-Host "  Waiting for deletion (this may take 15-20 min)..."
    aws cloudformation wait stack-delete-complete --stack-name $STACK_NAME --region $REGION 2>$null

    $STATUS = aws cloudformation describe-stacks `
        --stack-name $STACK_NAME `
        --query "Stacks[0].StackStatus" --output text `
        --region $REGION 2>$null

    if ($STATUS -eq "DELETE_FAILED") {
        Write-Host "  Deletion failed, retrying with retained resources..." -ForegroundColor Yellow
        $FAILED = aws cloudformation describe-stack-resources `
            --stack-name $STACK_NAME `
            --query "StackResources[?ResourceStatus=='DELETE_FAILED'].LogicalResourceId" `
            --output text --region $REGION
        Write-Host "  Retaining: $FAILED"
        aws cloudformation delete-stack `
            --stack-name $STACK_NAME `
            --retain-resources $FAILED.Split() `
            --region $REGION
        aws cloudformation wait stack-delete-complete --stack-name $STACK_NAME --region $REGION 2>$null
    }
}

# ── Clean up ENIs ──
Write-Host "[2/7] Cleaning orphaned ENIs..." -ForegroundColor Cyan
if ($VPC_ID -and $VPC_ID -ne "None") {
    $ENIS = aws ec2 describe-network-interfaces `
        --filters "Name=vpc-id,Values=$VPC_ID" `
        --query "NetworkInterfaces[].[NetworkInterfaceId,Status,Attachment.AttachmentId]" `
        --output text --region $REGION 2>$null

    if ($ENIS) {
        $ENIS -split "`n" | ForEach-Object {
            $parts = $_.Trim() -split "\t"
            $eniId = $parts[0]
            $status = $parts[1]
            $attachId = $parts[2]

            Write-Host "  ENI $eniId ($status)"
            if ($status -eq "in-use" -and $attachId -and $attachId -ne "None") {
                aws ec2 detach-network-interface --attachment-id $attachId --force --region $REGION 2>$null
                Start-Sleep -Seconds 5
            }
            aws ec2 delete-network-interface --network-interface-id $eniId --region $REGION 2>$null
        }
    } else {
        Write-Host "  No ENIs found"
    }
} else {
    Write-Host "  No VPC to clean"
}

# ── Clean up VPC ──
Write-Host "[3/7] Cleaning VPC resources..." -ForegroundColor Cyan
if ($VPC_ID -and $VPC_ID -ne "None") {
    $vpcExists = aws ec2 describe-vpcs --vpc-ids $VPC_ID --region $REGION 2>$null
    if ($vpcExists) {
        # Security groups
        $SGS = aws ec2 describe-security-groups `
            --filters "Name=vpc-id,Values=$VPC_ID" `
            --query "SecurityGroups[?GroupName!='default'].GroupId" `
            --output text --region $REGION 2>$null
        if ($SGS) {
            $SGS.Split() | ForEach-Object {
                Write-Host "  Deleting SG $_"
                aws ec2 delete-security-group --group-id $_ --region $REGION 2>$null
            }
        }

        # Subnets
        $SUBS = aws ec2 describe-subnets `
            --filters "Name=vpc-id,Values=$VPC_ID" `
            --query "Subnets[].SubnetId" --output text `
            --region $REGION 2>$null
        if ($SUBS) {
            $SUBS.Split() | ForEach-Object {
                Write-Host "  Deleting subnet $_"
                aws ec2 delete-subnet --subnet-id $_ --region $REGION 2>$null
            }
        }

        # Internet gateways
        $IGWS = aws ec2 describe-internet-gateways `
            --filters "Name=attachment.vpc-id,Values=$VPC_ID" `
            --query "InternetGateways[].InternetGatewayId" `
            --output text --region $REGION 2>$null
        if ($IGWS) {
            $IGWS.Split() | ForEach-Object {
                aws ec2 detach-internet-gateway --internet-gateway-id $_ --vpc-id $VPC_ID --region $REGION 2>$null
                Write-Host "  Deleting IGW $_"
                aws ec2 delete-internet-gateway --internet-gateway-id $_ --region $REGION 2>$null
            }
        }

        # Route tables (non-main)
        $RTS = aws ec2 describe-route-tables `
            --filters "Name=vpc-id,Values=$VPC_ID" `
            --query "RouteTables[?Associations[0].Main!=``true``].RouteTableId" `
            --output text --region $REGION 2>$null
        if ($RTS) {
            $RTS.Split() | ForEach-Object {
                $rt = $_
                $ASSOCS = aws ec2 describe-route-tables `
                    --route-table-ids $rt `
                    --query "RouteTables[0].Associations[?!Main].RouteTableAssociationId" `
                    --output text --region $REGION 2>$null
                if ($ASSOCS) {
                    $ASSOCS.Split() | ForEach-Object {
                        aws ec2 disassociate-route-table --association-id $_ --region $REGION 2>$null
                    }
                }
                Write-Host "  Deleting route table $rt"
                aws ec2 delete-route-table --route-table-id $rt --region $REGION 2>$null
            }
        }

        # VPC
        Write-Host "  Deleting VPC $VPC_ID"
        aws ec2 delete-vpc --vpc-id $VPC_ID --region $REGION 2>$null
    } else {
        Write-Host "  VPC already deleted"
    }
} else {
    Write-Host "  No VPC to clean"
}

# ── Clean up Secrets ──
Write-Host "[4/7] Purging Secrets Manager..." -ForegroundColor Cyan
foreach ($SECRET in @("$STACK_NAME/mysql", "$STACK_NAME/docdb", "$STACK_NAME/jwt-secret", "$STACK_NAME/db-encryption-key")) {
    $exists = aws secretsmanager describe-secret --secret-id $SECRET --region $REGION 2>$null
    if ($exists) {
        Write-Host "  Deleting: $SECRET"
        aws secretsmanager delete-secret `
            --secret-id $SECRET `
            --force-delete-without-recovery `
            --region $REGION 2>$null
    }
}

# ── Clean up S3 ──
Write-Host "[5/7] Deleting S3 buckets..." -ForegroundColor Cyan
foreach ($BUCKET in @(
    "$STACK_NAME-images-$ACCOUNT_ID-$REGION",
    "$STACK_NAME-cfn-artifacts-$ACCOUNT_ID-$REGION"
)) {
    $exists = aws s3api head-bucket --bucket $BUCKET --region $REGION 2>$null
    if ($?) {
        Write-Host "  Deleting: $BUCKET"
        aws s3 rm "s3://$BUCKET" --recursive --region $REGION 2>$null
        aws s3 rb "s3://$BUCKET" --region $REGION 2>$null
    }
}

# ── Clean up ECR ──
Write-Host "[6/7] Deleting ECR repository..." -ForegroundColor Cyan
$REPO = "$STACK_NAME-app"
$ecrExists = aws ecr describe-repositories --repository-names $REPO --region $REGION 2>$null
if ($ecrExists) {
    Write-Host "  Deleting: $REPO"
    aws ecr delete-repository --repository-name $REPO --force --region $REGION 2>$null
}

# ── Summary ──
Write-Host ""
Write-Host "[7/7] Verifying..." -ForegroundColor Cyan
$REMAINING = aws cloudformation describe-stacks --stack-name $STACK_NAME --region $REGION 2>$null
if (-not $REMAINING) {
    Write-Host ""
    Write-Host "===============================" -ForegroundColor Green
    Write-Host "  Environment destroyed" -ForegroundColor Green
    Write-Host "===============================" -ForegroundColor Green
    Write-Host "Ready for a clean re-deploy."
} else {
    Write-Host ""
    Write-Host "===============================" -ForegroundColor Red
    Write-Host "  Some resources may remain" -ForegroundColor Red
    Write-Host "===============================" -ForegroundColor Red
    Write-Host "Run this script again to retry."
}
