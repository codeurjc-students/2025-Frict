import json
import urllib.request
import urllib.parse
import urllib.error

TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo"


def lambda_handler(event, context):
    token = event.get("token", "")
    client_id = event.get("clientId", "")

    if not token:
        return {"errorMessage": "token is required"}

    url = f"{TOKENINFO_URL}?id_token={urllib.parse.quote(token, safe='')}"

    try:
        with urllib.request.urlopen(url, timeout=8) as resp:
            data = json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        try:
            error_body = json.loads(e.read().decode())
            return {"errorMessage": error_body.get("error_description", "Invalid token")}
        except Exception:
            return {"errorMessage": "Token validation failed"}
    except Exception as e:
        return {"errorMessage": f"Verification error: {e}"}

    if client_id and data.get("aud") != client_id:
        return {"errorMessage": "Token audience mismatch"}

    if data.get("email_verified") not in ("true", True):
        return {"errorMessage": "Email not verified by Google"}

    return {
        "email": data.get("email", ""),
        "name": data.get("name", ""),
        "sub": data.get("sub", "")
    }
