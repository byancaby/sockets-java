package projeto1_sockets;

// Importa a classe Socket para instanciar a ponta do cliente na conexão TCP com o servidor
import java.net.Socket;
// Importa a classe InputStreamReader para converter o fluxo de bytes do socket em fluxo de caracteres UTF-8
import java.io.InputStreamReader;
// Importa a classe BufferedReader para ler dados de forma bufferizada linha a linha a partir do socket
import java.io.BufferedReader;
// Importa a classe PrintWriter para escrever texto formatado com quebra de linha no socket de saída
import java.io.PrintWriter;
// Importa a classe IOException para tratamento de exceções de comunicação e I/O de rede
import java.io.IOException;
// Importa a classe Scanner para leitura de comandos via teclado na interface textual do console
import java.util.Scanner;

/**
 * Cliente TCP da Calculadora Distribuída.
 * Evolução do projeto acadêmico JavaClientSocket_Calculadora.
 * 
 * Fornece interface textual para:
 *   1. Operações básicas individuais (+, -, *, /).
 *   2. Resolução de expressões via Abordagem 1 (Decomposição no cliente com múltiplas chamadas remotas).
 *   3. Resolução de expressões via Abordagem 2 (Envio da expressão completa para cálculo no servidor).
 *   4. Execução de teste automático comparativo com a expressão (10 + 15) * 4.
 * 
 * TODAS AS LINHAS RELACIONADAS À REDE FORAM COMENTADAS INDIVIDUALMENTE.
 */
public class JavaClientSocket_Calculadora {

    // Define o IP de loopback local para conexão com o processo servidor
    private static final String HOST = "127.0.0.1";
    // Define a porta TCP de destino configurada no servidor
    private static final int PORTA = 9876;

    // Referência do socket de conexão com o servidor
    private Socket socket;
    // Fluxo de leitura para capturar dados vindos do servidor pela rede
    private BufferedReader entradaRede;
    // Fluxo de envio para despachar dados ao servidor pela rede
    private PrintWriter saidaRede;

    /**
     * Estabelece a conexão com o servidor TCP via Three-Way Handshake.
     */
    public void conectar() throws IOException {
        // Instancia o Socket e executa a chamada connect() enviando o pacote TCP SYN ao servidor na porta 9876
        this.socket = new Socket(HOST, PORTA);
        // Configura o timeout de leitura no socket para 10000ms (10 segundos) prevenindo congelamento por perda de pacotes
        this.socket.setSoTimeout(10000);
        // Cria o BufferedReader sobre o InputStream do socket para ler respostas delimitadas por '\n'
        this.entradaRede = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        // Cria o PrintWriter com autoflush=true sobre o OutputStream do socket para despachar bytes imediatamente
        this.saidaRede = new PrintWriter(socket.getOutputStream(), true);
        // Imprime confirmação do canal de comunicação TCP ativo
        System.out.println("[REDE] Conexão TCP estabelecida com sucesso com " + HOST + ":" + PORTA);
    }

    /**
     * Envia uma mensagem em linha única pela rede e bloqueia até receber a resposta do servidor.
     */
    public synchronized String enviarMensagem(String mensagem) throws IOException {
        // Verifica se o socket está conectado e ativo antes de tentar transmitir
        if (socket == null || socket.isClosed()) {
            // Lança exceção de conexão inativa caso o socket não esteja pronto
            throw new IOException("Socket de rede não está conectado ao servidor.");
        }

        // Executa println() no PrintWriter, enviando a linha de texto e a quebra de linha ('\n') pelo buffer de saída TCP
        saidaRede.println(mensagem);
        // Imprime no console a confirmação do frame de dados que trafegou na rede em direção ao servidor
        System.out.println("  [REDE -> ENVIANDO]: " + mensagem);

        // Bloqueia a thread na chamada readLine() até que o servidor envie uma linha terminada em '\n' pela rede
        String resposta = entradaRede.readLine();

        // Se readLine() retornar null, o servidor fechou o socket enviando um pacote TCP FIN
        if (resposta == null) {
            // Lança exceção informando encerramento inesperado do canal TCP
            throw new IOException("O servidor fechou a conexão TCP abruptamente.");
        }

        // Imprime no console a confirmação da recepção da resposta pela rede
        System.out.println("  [REDE <- RECEBIDO]: " + resposta);

        // Retorna a linha de resposta recebida do servidor
        return resposta;
    }

    /**
     * Encerra a conexão com o servidor e libera descritores de rede.
     */
    public void desconectar() {
        try {
            // Se o socket estiver aberto, envia comando de finalização graciosa pela rede
            if (saidaRede != null) {
                // Envia comando textual SAIR pelo canal TCP
                saidaRede.println("SAIR");
            }
        } catch (Exception ignored) {
        } finally {
            try {
                // Fecha o fluxo de leitura da rede
                if (entradaRede != null) entradaRede.close();
                // Fecha o fluxo de escrita da rede
                if (saidaRede != null) saidaRede.close();
                // Executa close() no socket TCP enviando o pacote de encerramento TCP FIN para o servidor
                if (socket != null && !socket.isClosed()) socket.close();
                // Registra encerramento no terminal
                System.out.println("[REDE] Conexão TCP com o servidor finalizada.");
            } catch (IOException e) {
                // Trata exceção durante o encerramento do socket
                System.err.println("[REDE] Erro ao fechar o socket TCP: " + e.getMessage());
            }
        }
    }

    /**
     * Executa uma operação básica individual chamando o servidor via socket TCP.
     */
    public double executarOperacaoRemota(String operador, double a, double b) throws Exception {
        String comando = "OP " + operador + " " + a + " " + b;
        String resposta = enviarMensagem(comando);

        if (resposta.startsWith("OK ")) {
            return Double.parseDouble(resposta.substring(3).trim());
        } else {
            throw new RuntimeException("Falha na operação remota: " + resposta);
        }
    }

    /**
     * ABORDAGEM 1 (Decomposição no Cliente):
     * O cliente faz o parsing da expressão matemática (respeitando parênteses e precedência)
     * e, para cada operador binário, executa uma chamada remota individual ao servidor.
     */
    public double calcularAbordagem1(String expressao) throws Exception {
        System.out.println("\n--- [ABORDAGEM 1: Decomposição no Cliente com Chamadas Remotas] ---");
        System.out.println("Expressão a decompor: " + expressao);

        // Define a função de callback que será chamada pelo avaliador para cada nó binário
        ExpressionEvaluator.RemoteOperationCallback callback = (op, a, b) -> {
            System.out.println("  -> Cliente invocando servidor para nó atômico: " + a + " " + op + " " + b);
            double resultadoParcial = executarOperacaoRemota(op, a, b);
            System.out.println("  <- Servidor retornou resultado parcial: " + resultadoParcial);
            return resultadoParcial;
        };

        // O avaliador decompõe a expressão chamando o servidor para cada operação
        return ExpressionEvaluator.evaluate(expressao, callback);
    }

    /**
     * ABORDAGEM 2 (Processamento no Servidor):
     * O cliente envia a string da expressão inteira pela rede e o servidor a resolve completamente.
     */
    public double calcularAbordagem2(String expressao) throws Exception {
        System.out.println("\n--- [ABORDAGEM 2: Processamento Integral no Servidor] ---");
        System.out.println("Expressão enviada na íntegra: " + expressao);

        String comando = "EXPR " + expressao;
        String resposta = enviarMensagem(comando);

        if (resposta.startsWith("OK ")) {
            return Double.parseDouble(resposta.substring(3).trim());
        } else {
            throw new RuntimeException("Falha no cálculo da expressão remota: " + resposta);
        }
    }

    private static void exibirMenu() {
        System.out.println("\n=======================================================");
        System.out.println("        CALCULADORA DISTRIBUÍDA - SOCKETS TCP (JAVA)   ");
        System.out.println("=======================================================");
        System.out.println("1 - Operação Aritmética Básica Remota (+, -, *, /)");
        System.out.println("2 - Resolver Expressão [Abordagem 1 - Decomposição no Cliente]");
        System.out.println("3 - Resolver Expressão [Abordagem 2 - Cálculo no Servidor]");
        System.out.println("4 - Executar Teste Automático com Exemplo: (10 + 15) * 4");
        System.out.println("5 - Sair");
        System.out.print("Escolha uma opção (1-5): ");
    }

    public static void main(String[] args) {
        JavaClientSocket_Calculadora cliente = new JavaClientSocket_Calculadora();
        Scanner scanner = new Scanner(System.in);

        try {
            cliente.conectar();
        } catch (IOException e) {
            System.err.println("\n[ERRO DE CONEXÃO] Não foi possível conectar ao servidor TCP em " + HOST + ":" + PORTA);
            System.err.println("Certifique-se de iniciar 'JavaServerSocket_Calculadora' primeiro.");
            System.err.println("Detalhe: " + e.getMessage());
            return;
        }

        boolean executando = true;
        while (executando) {
            try {
                exibirMenu();
                String opcao = scanner.nextLine().trim();

                switch (opcao) {
                    case "1":
                        System.out.print("Digite o operador (+, -, *, /): ");
                        String op = scanner.nextLine().trim();
                        System.out.print("Digite o primeiro número (A): ");
                        double a = Double.parseDouble(scanner.nextLine().trim());
                        System.out.print("Digite o segundo número (B): ");
                        double b = Double.parseDouble(scanner.nextLine().trim());

                        double resOp = cliente.executarOperacaoRemota(op, a, b);
                        System.out.println("\n>>> RESULTADO REMOTO: " + a + " " + op + " " + b + " = " + resOp);
                        break;

                    case "2":
                        System.out.print("Digite a expressão matemática com parênteses (ex: (10 + 15) * 4): ");
                        String expr1 = scanner.nextLine().trim();
                        double resA = cliente.calcularAbordagem1(expr1);
                        System.out.println("\n>>> RESULTADO FINAL (Abordagem 1): " + expr1 + " = " + resA);
                        break;

                    case "3":
                        System.out.print("Digite a expressão matemática com parênteses (ex: (10 + 15) * 4): ");
                        String expr2 = scanner.nextLine().trim();
                        double resB = cliente.calcularAbordagem2(expr2);
                        System.out.println("\n>>> RESULTADO FINAL (Abordagem 2): " + expr2 + " = " + resB);
                        break;

                    case "4":
                        String exemplo = "(10 + 15) * 4";
                        System.out.println("\n=======================================================");
                        System.out.println("  TESTE COMPARATIVO COM A EXPRESSÃO: " + exemplo);
                        System.out.println("=======================================================");

                        double teste1 = cliente.calcularAbordagem1(exemplo);
                        System.out.println("Resultado da Abordagem 1: " + teste1);

                        double teste2 = cliente.calcularAbordagem2(exemplo);
                        System.out.println("Resultado da Abordagem 2: " + teste2);

                        if (teste1 == 100.0 && teste2 == 100.0) {
                            System.out.println("\n>>> [SUCESSO TOTAL] Ambas as abordagens retornaram 100.0 perfeitamente!");
                        } else {
                            System.err.println("\n>>> [ERRO] Resultados divergentes no teste!");
                        }
                        break;

                    case "5":
                        System.out.println("Finalizando cliente...");
                        executando = false;
                        break;

                    default:
                        System.out.println("Opção inválida! Digite de 1 a 5.");
                        break;
                }
            } catch (Exception e) {
                System.err.println("\n[ERRO]: " + e.getMessage());
            }
        }

        cliente.desconectar();
        scanner.close();
    }
}
