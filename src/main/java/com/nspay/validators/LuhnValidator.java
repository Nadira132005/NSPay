package com.nspay.validators;
import org.springframework.stereotype.Component;

@Component
public class LuhnValidator {

    public boolean isValid(String cardNumber) {
        int sum = 0;
        if(cardNumber.length() != 16)
        {
            return false;
        }

        for (int i =0; i < cardNumber.length(); i++)
        {
            int n = Integer.parseInt(cardNumber.substring(i, i + 1));
            if (i%2==0)
            {
                n *= 2;
                if (n > 9)
                {
                    n = (n % 10) + n/10;
                }
            }
            sum += n;
        }
        return (sum % 10 == 0);

    }
}
