import {AbstractControl, AsyncValidatorFn, ValidationErrors, ValidatorFn} from '@angular/forms';
import {UserService} from '../services/user.service';
import {map, Observable, of, switchMap, timer} from 'rxjs';


export class CustomValidators {

  static createUsernameValidator(userService: UserService): AsyncValidatorFn {
    return (control: AbstractControl): Observable<ValidationErrors | null> => {
      const value = control.value;

      if (!value) {
        return of(null);
      }

      return timer(750).pipe(
        switchMap(() => userService.checkUsernameTaken(value)),
        // Error if it exists
        map((isTaken) => (isTaken ? { usernameTaken: true } : null))
      );
    };
  }

  static createEmailValidator(userService: UserService): AsyncValidatorFn {
    return (control: AbstractControl): Observable<ValidationErrors | null> => {
      const value = control.value;
      if (!value) return of(null);

      return timer(750).pipe(
        switchMap(() => userService.checkEmailTaken(value)),
        map((isTaken) => (isTaken ? { emailTaken: true } : null))
      );
    };
  }

  //Check if the username exists
  static createUsernameExistsValidator(userService: UserService): AsyncValidatorFn {
    return (control: AbstractControl): Observable<ValidationErrors | null> => {
      const value = control.value;
      if (!value) return of(null);

      return timer(750).pipe(
        switchMap(() => userService.checkUsernameTaken(value)),
        // Error if it does not exist
        map((exists) => (exists ? null : { userNotFound: true }))
      );
    };
  }

  //Check if the passwords match
  static passwordMatchValidator: ValidatorFn = (group: AbstractControl): ValidationErrors | null => {
    const password = group.get('password')?.value;
    const repeatPassword = group.get('repeatPassword')?.value;

    // Si no coinciden y ambos tienen valor, devolvemos error
    return password === repeatPassword ? null : { mismatch: true };
  };
}
