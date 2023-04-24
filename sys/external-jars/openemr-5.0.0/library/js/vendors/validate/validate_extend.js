/**
 * LICENSE: This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see
 * http://www.gnu.org/licenses/licenses.html#GPL .
 *
 * @package OpenEMR
 * @author  Sharon Cohen <sharonco@matrix.co.il>
 * @author  Amiel Elboim <amielel@matrix.co.il>
 * @link    http://www.open-emr.org
 */

// Extend for date roll from  https://validatejs.org/#validators-datetime

// Before using it we must add the parse and format functions
// Here is a sample implementation using moment.js
validate.extend(validate.validators.datetime, {
    // The value is guaranteed not to be null or undefined but otherwise it
    // could be anything.
    parse: function(value, options) {
        return +moment.utc(value);
    },
    // Input is a unix timestamp
    format: function(value, options) {
        var format = options.dateOnly ? "YYYY-MM-DD" : "YYYY-MM-DD hh:mm:ss";
        return moment.utc(value).format(format);
    }
});


/*
*  Custom validator documentation - https://validatejs.org/#custom-validator
*/

/**
* validate that date is past date, recommended to put it after {date: {dateOnly: true}}
* you can specify the message option {onlyPast:{message:'text example'}}
* optional options -
* 1)(string) massage
* 2)(boolean) onlyYear
*/
validate.validators.pastDate = function(value, options) {
    //exit if empty value
    if(validate.isEmpty(value)) { return;}
    // exit if options = false
    if(!options) return;
    //Without this fix an empty date doesn't pass validation because value in DB is "0000-00-00".
    if(value == "0000-00-00"){
        return;
    }
    var now = new Date().getTime();
    //if set onlyYear option
    if (options.onlyYear != undefined && options.onlyYear) {
        if(value < 1800 || value > now.getFullYear ) {

            return throwError('Must be year format');
        }
    }

    var date =  new Date(value);
    var mls_date = date.getTime();
    if(isNaN(mls_date)) {
       return throwError('Must be valid date');
    }

    if(now < mls_date) {

       return throwError( 'Must be past date');
    }

    // throw error
    function throwError(message){
        if(validate.isObject(options) && options.message != undefined) {
            return options.message;
        } else {
            return message;
        }
    }
};


/**
 * Luhn algorithm in JavaScript: validate credit card number supplied as string of numbers
 * @author ShirtlessKirk. https://gist.github.com/ShirtlessKirk/2134376
 * you can specify the message option {luhn:{message:'text example'}}
 *
 */

validate.validators.luhn = function(value, options) {

    //calculate Luhn algorithm
    var luhnChk = (function (arr) {
        return function (ccNum) {
            var
                len = ccNum.length,
                bit = 1,
                sum = 0,
                val;

            while (len) {
                val = parseInt(ccNum.charAt(--len), 10);
                sum += (bit ^= 1) ? arr[val] : val;
            }

            return sum && sum % 10 === 0;
        };
    }([0, 2, 4, 6, 8, 1, 3, 5, 7, 9]));

    //exit if empty value
    if(validate.isEmpty(value)) { return; }
    // exit if options = false
    if(!options) return;

    var valid = luhnChk(value);

    if(!valid) {
        if(validate.isObject(options) && options.message != undefined) {
            return options.message;
        } else {
            return 'Invalid luhn algorithm';
        }
    }
}



