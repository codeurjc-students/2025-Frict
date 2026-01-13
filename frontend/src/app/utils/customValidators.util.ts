import {AbstractControl, AsyncValidatorFn, ValidationErrors} from '@angular/forms';
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
}
