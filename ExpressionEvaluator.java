package projeto1_sockets;

import java.util.*;

/**
 * Utilitário de parsing e avaliação de expressões matemáticas com suporte
 * a parênteses e operadores (+, -, *, /) respeitando a precedência aritmética.
 * 
 * Implementa o algoritmo Shunting-Yard de Dijkstra para conversão de Notação Infixa
 * para Notação Polonesa Reversa (RPN / Posfixa), permitindo:
 *  - Avaliação local direta (usada no Servidor - Abordagem 2).
 *  - Avaliação distribuída/decomposta via callback (usada no Cliente - Abordagem 1).
 */
public class ExpressionEvaluator {

    /**
     * Interface funcional para delegar a execução de operações binárias
     * para o servidor remoto através da rede (Abordagem 1).
     */
    @FunctionalInterface
    public interface RemoteOperationCallback {
        double execute(String operator, double a, double b) throws Exception;
    }

    /**
     * Retorna a precedência matemática do operador.
     */
    private static int getPrecedence(String op) {
        switch (op) {
            case "+":
            case "-":
                return 1;
            case "*":
            case "/":
                return 2;
            default:
                return -1;
        }
    }

    /**
     * Verifica se o token representa um dos quatro operadores básicos.
     */
    private static boolean isOperator(String token) {
        return token.equals("+") || token.equals("-") || token.equals("*") || token.equals("/");
    }

    /**
     * Converte a expressão de string infixa para lista de tokens posfixos (RPN)
     * utilizando o algoritmo Shunting-Yard.
     */
    public static List<String> infixToRPN(String expression) {
        List<String> outputQueue = new ArrayList<>();
        Stack<String> operatorStack = new Stack<>();
        
        // Normaliza a expressão inserindo espaços ao redor de parênteses e operadores
        StringBuilder spaced = new StringBuilder();
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c == '(' || c == ')' || c == '+' || c == '*' || c == '/') {
                spaced.append(" ").append(c).append(" ");
            } else if (c == '-') {
                // Trata o sinal de menos (pode ser operador binário ou número negativo)
                if (spaced.toString().trim().isEmpty() || 
                    spaced.toString().trim().endsWith("(") || 
                    spaced.toString().trim().endsWith("+") || 
                    spaced.toString().trim().endsWith("-") || 
                    spaced.toString().trim().endsWith("*") || 
                    spaced.toString().trim().endsWith("/")) {
                    spaced.append(" -"); // número negativo
                } else {
                    spaced.append(" - "); // operador de subtração
                }
            } else {
                spaced.append(c);
            }
        }

        String[] tokens = spaced.toString().trim().split("\\s+");

        for (String token : tokens) {
            if (token.isEmpty()) continue;

            if (isOperator(token)) {
                while (!operatorStack.isEmpty() && isOperator(operatorStack.peek()) &&
                        getPrecedence(operatorStack.peek()) >= getPrecedence(token)) {
                    outputQueue.add(operatorStack.pop());
                }
                operatorStack.push(token);
            } else if (token.equals("(")) {
                operatorStack.push(token);
            } else if (token.equals(")")) {
                while (!operatorStack.isEmpty() && !operatorStack.peek().equals("(")) {
                    outputQueue.add(operatorStack.pop());
                }
                if (!operatorStack.isEmpty() && operatorStack.peek().equals("(")) {
                    operatorStack.pop(); // Descarta o parêntese '('
                } else {
                    throw new IllegalArgumentException("Parênteses desbalanceados na expressão.");
                }
            } else {
                // É um número (operando)
                try {
                    Double.parseDouble(token);
                    outputQueue.add(token);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Token inválido na expressão: " + token);
                }
            }
        }

        while (!operatorStack.isEmpty()) {
            String op = operatorStack.pop();
            if (op.equals("(") || op.equals(")")) {
                throw new IllegalArgumentException("Parênteses desbalanceados na expressão.");
            }
            outputQueue.add(op);
        }

        return outputQueue;
    }

    /**
     * Avalia a expressão RPN.
     * Se callback for null, computa localmente (Servidor - Abordagem 2).
     * Se callback for fornecido, delega cada operação binária para a rede (Cliente - Abordagem 1).
     */
    public static double evaluateRPN(List<String> rpnTokens, RemoteOperationCallback callback) throws Exception {
        Stack<Double> evaluationStack = new Stack<>();

        for (String token : rpnTokens) {
            if (!isOperator(token)) {
                evaluationStack.push(Double.parseDouble(token));
            } else {
                if (evaluationStack.size() < 2) {
                    throw new IllegalArgumentException("Expressão mal formatada.");
                }
                double b = evaluationStack.pop();
                double a = evaluationStack.pop();
                double result;

                if (callback != null) {
                    // Abordagem 1: chamada remota para o servidor
                    result = callback.execute(token, a, b);
                } else {
                    // Abordagem 2: cálculo local no servidor
                    switch (token) {
                        case "+": result = a + b; break;
                        case "-": result = a - b; break;
                        case "*": result = a * b; break;
                        case "/":
                            if (b == 0) throw new ArithmeticException("Divisão por zero não é permitida.");
                            result = a / b;
                            break;
                        default:
                            throw new IllegalArgumentException("Operador desconhecido: " + token);
                    }
                }
                evaluationStack.push(result);
            }
        }

        if (evaluationStack.size() != 1) {
            throw new IllegalArgumentException("Expressão mal formatada.");
        }

        return evaluationStack.pop();
    }

    /**
     * Método de conveniência para avaliar diretamente uma expressão em string.
     */
    public static double evaluate(String expression, RemoteOperationCallback callback) throws Exception {
        List<String> rpn = infixToRPN(expression);
        return evaluateRPN(rpn, callback);
    }
}
