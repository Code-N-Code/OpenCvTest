package com.codencode.opencvtest;

import java.util.Stack;

import static java.lang.Character.isDigit;

public class ZeroDegreeSolver {
    ZeroDegreeSolver(){}

    private boolean isOperator(char ch)
    {
        if(ch == '-' || ch == '+' || ch == '*' || ch == '^')
            return true;
        else
            return false;
    }

    private boolean hasHigherPrecedence(char opA , char opB)
    {
        if((opB == '+' || opB == '-') && (opA == '*' || opA == '/'))
            return true;

        return false;
    }

    private double applyOperator(double a , double b , char _operator)
    {
        if(_operator == '+') return a + b;
        else
        if(_operator == '-') return a - b;
        else
        if(_operator == '*') return a * b;
        else
        if(_operator == '/') return a / b;

        return 0;
    }

    double solve(String exp)
    {
        Stack<Double> expStack = new Stack<>();
        Stack<Character> operatorStack = new Stack<>();

        for(int i=0;i<exp.length();)
        {
            char ch = exp.charAt(i);

            if(ch == '(')
            {
                operatorStack.push(ch);
            }
            else
            if(isOperator(ch))
            {
                while(!operatorStack.empty() && isOperator(operatorStack.peek()) && hasHigherPrecedence(operatorStack.peek() , ch))
                {
                    double b = expStack.pop();
                    double a = expStack.pop();
                    char _operator = operatorStack.pop();
                    expStack.push(applyOperator(a , b , _operator));
                }
                operatorStack.push(ch);
            }
            else
            if(isDigit(ch))
            {
                int num = ch - '0';
                while((i + 1 < exp.length()) && isDigit(exp.charAt(i+1)))
                {
                    num = 10 * num + exp.charAt(i+1) - '0';
                    i++;
                }
                expStack.push((double) num);
            }
            else
            if(ch == ')')
            {
                while(true)
                {
                    double b = expStack.pop();
                    double a = expStack.pop();
                    char _operator = operatorStack.pop();
                    expStack.push(applyOperator(a , b , _operator));

                    if(operatorStack.peek() == '(')
                    {
                        operatorStack.pop();
                        break;
                    }
                }
            }
            i++;
        }

        while(!operatorStack.empty())
        {
            double a = 0 , b;
            b = expStack.pop();

            if(expStack.empty() == false)
                a = expStack.pop();

            char _operator = operatorStack.pop();
            expStack.push(applyOperator(a , b , _operator));
        }

        return expStack.peek();
    }

}
