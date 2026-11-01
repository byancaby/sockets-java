package projeto1_sockets;

public class TesteSockets {
    public static void main(String[] args) throws Exception {
        // Inicia o servidor em uma thread em background
        Thread servidorThread = new Thread(() -> {
            JavaServerSocket_Calculadora.main(new String[]{});
        });
        servidorThread.setDaemon(true);
        servidorThread.start();

        // Aguarda 1 segundo para o ServerSocket entrar em modo listen()
        Thread.sleep(1000);

        System.out.println("\n[INICIANDO TESTES DO PROJETO 1 - SOCKETS TCP]");
        JavaClientSocket_Calculadora cliente = new JavaClientSocket_Calculadora();
        cliente.conectar();

        // 1. Operações básicas
        double soma = cliente.executarOperacaoRemota("+", 10, 15);
        System.out.println("Teste Soma (10 + 15): " + soma + " -> " + (soma == 25.0 ? "OK" : "FALHA"));

        double sub = cliente.executarOperacaoRemota("-", 50, 20);
        System.out.println("Teste Subtração (50 - 20): " + sub + " -> " + (sub == 30.0 ? "OK" : "FALHA"));

        double mult = cliente.executarOperacaoRemota("*", 25, 4);
        System.out.println("Teste Multiplicação (25 * 4): " + mult + " -> " + (mult == 100.0 ? "OK" : "FALHA"));

        double div = cliente.executarOperacaoRemota("/", 100, 4);
        System.out.println("Teste Divisão (100 / 4): " + div + " -> " + (div == 25.0 ? "OK" : "FALHA"));

        // 2. Expressão com parênteses - Abordagem 1
        String expressao = "(10 + 15) * 4";
        double resA1 = cliente.calcularAbordagem1(expressao);
        System.out.println("Teste Abordagem 1: " + expressao + " = " + resA1 + " -> " + (resA1 == 100.0 ? "OK" : "FALHA"));

        // 3. Expressão com parênteses - Abordagem 2
        double resA2 = cliente.calcularAbordagem2(expressao);
        System.out.println("Teste Abordagem 2: " + expressao + " = " + resA2 + " -> " + (resA2 == 100.0 ? "OK" : "FALHA"));

        // 4. Expressão aninhada mais complexa
        String complexa = "((20 - 5) * 2) / 3";
        double compA1 = cliente.calcularAbordagem1(complexa);
        double compA2 = cliente.calcularAbordagem2(complexa);
        System.out.println("Teste Complexo Abordagem 1: " + complexa + " = " + compA1);
        System.out.println("Teste Complexo Abordagem 2: " + complexa + " = " + compA2);

        cliente.desconectar();

        if (resA1 == 100.0 && resA2 == 100.0 && compA1 == 10.0 && compA2 == 10.0) {
            System.out.println("\n>>> [SUCESSO ABSOLUTO] TODOS OS TESTES DO PROJETO 1 PASSARAM! <<<\n");
        } else {
            System.err.println("\n>>> [FALHA] Algum teste falhou!");
            System.exit(1);
        }
        System.exit(0);
    }
}
