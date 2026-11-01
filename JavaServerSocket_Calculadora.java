package projeto1_sockets;

// Importa a classe ServerSocket para criar o socket servidor que escuta conexões TCP de entrada
import java.net.ServerSocket;
// Importa a classe Socket para gerenciar o canal de comunicação bidirecional com cada cliente conectado
import java.net.Socket;
// Importa a classe InputStreamReader para decodificar os bytes recebidos da rede em caracteres legíveis
import java.io.InputStreamReader;
// Importa a classe BufferedReader para permitir a leitura eficiente linha a linha do buffer de recepção
import java.io.BufferedReader;
// Importa a classe PrintWriter para envio formatado de texto com quebra de linha pelo socket
import java.io.PrintWriter;
// Importa IOException para tratamento de falhas de entrada e saída na camada de transporte
import java.io.IOException;
// Importa a classe SocketException para captura de eventos específicos de desconexão na camada de socket
import java.net.SocketException;

/**
 * Servidor TCP Multithread da Calculadora Distribuída.
 * Evolução do projeto acadêmico JavaServerSocket_Calculadora.
 * 
 * Atende clientes de forma concorrente, suportando operações isoladas (+, -, *, /)
 * e o cálculo de expressões completas no servidor (Abordagem 2).
 * 
 * TODAS AS LINHAS RELACIONADAS À REDE FORAM COMENTADAS INDIVIDUALMENTE.
 */
public class JavaServerSocket_Calculadora {

    // Define a porta lógica TCP na qual o processo servidor ficará escutando
    private static final int PORTA = 9876;

    public static void main(String[] args) {
        // Declara a referência do ServerSocket que atuará na camada de transporte
        ServerSocket servidorSocket = null;
        try {
            // Instancia o ServerSocket e realiza a chamada de sistema bind() na porta 9876, iniciando o listen()
            servidorSocket = new ServerSocket(PORTA);
            // Imprime no console local a confirmação de que o serviço está ativo e pronto na interface de rede
            System.out.println("=================================================================");
            // Notifica o endereço e a porta lógica alocada para o serviço
            System.out.println(">>> Servidor TCP da Calculadora ativo na porta " + PORTA + " <<<");
            // Informa que o servidor está aguardando conexões dos clientes
            System.out.println("Aguardando requisições de clientes...");
            // Exibe separador estético no terminal
            System.out.println("=================================================================");

            // Mantém o servidor em execução contínua para aceitar múltiplas conexões
            while (true) {
                // Bloqueia a execução até que o Three-Way Handshake TCP (SYN, SYN-ACK, ACK) seja concluído por um cliente
                Socket socketCliente = servidorSocket.accept();
                // Imprime no log o IP remoto e a porta efêmera atribuída pelo sistema operacional do cliente conectado
                System.out.println("[CONEXÃO] Cliente conectado: " + socketCliente.getInetAddress().getHostAddress() + ":" + socketCliente.getPort());

                // Dispara uma nova thread para atender o cliente em paralelo, liberando o ServerSocket para novos accept()
                new Thread(new ClientHandler(socketCliente)).start();
            }
        } catch (IOException e) {
            // Trata eventuais erros de I/O ao abrir ou operar o ServerSocket (ex: porta já em uso)
            System.err.println("[ERRO NO SERVIDOR] Falha no socket servidor: " + e.getMessage());
        } finally {
            // Bloco de encerramento seguro dos recursos
            if (servidorSocket != null && !servidorSocket.isClosed()) {
                try {
                    // Executa a chamada close() no ServerSocket para liberar a porta lógica 9876 no kernel do SO
                    servidorSocket.close();
                } catch (IOException e) {
                    // Trata eventual exceção durante o fechamento do ServerSocket
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Classe interna responsável por processar as mensagens trocadas na conexão com um cliente específico.
     */
    private static class ClientHandler implements Runnable {
        // Mantém a referência do socket TCP dedicado ao cliente associado a esta thread
        private final Socket socket;

        // Construtor que recebe o socket do cliente estabelecido no accept()
        public ClientHandler(Socket socket) {
            // Atribui o socket conectado ao atributo da instância
            this.socket = socket;
        }

        @Override
        public void run() {
            // Declara a referência do leitor de fluxo de entrada conectado ao buffer de recepção TCP
            BufferedReader entrada = null;
            // Declara a referência do escritor de fluxo de saída conectado ao buffer de transmissão TCP
            PrintWriter saida = null;

            try {
                // Envolve o InputStream do socket em um InputStreamReader com UTF-8 e em um BufferedReader para leitura de linhas
                entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                // Envolve o OutputStream do socket em um PrintWriter com autoflush ativado (descarrega bytes na rede a cada println)
                saida = new PrintWriter(socket.getOutputStream(), true);

                // Variável para armazenar cada linha textual recebida pela rede
                String linhaRequisicao;

                // Loop de leitura: bloqueia aguardando a chegada de bytes terminados por '\n' transmitidos pelo cliente
                while ((linhaRequisicao = entrada.readLine()) != null) {
                    // Imprime no console a mensagem bruta recebida do cliente pela rede
                    System.out.println("[REDE <- RECEBIDO de " + socket.getPort() + "]: " + linhaRequisicao);

                    // Trata comando de finalização graciosa da conexão
                    if (linhaRequisicao.equalsIgnoreCase("SAIR") || linhaRequisicao.equalsIgnoreCase("QUIT")) {
                        // Envia ao cliente mensagem de confirmação de encerramento da sessão
                        saida.println("OK Conexão encerrada.");
                        // Interrompe o loop de leitura para fechar os recursos do socket
                        break;
                    }

                    // Processa a requisição e computa a resposta
                    String resposta = processarComando(linhaRequisicao);

                    // Envia a resposta pela rede chamando println(), que despacha os bytes pelo buffer TCP com flush imediato
                    saida.println(resposta);
                    // Imprime no console local a confirmação da transmissão da resposta
                    System.out.println("[REDE -> ENVIADO para " + socket.getPort() + "]: " + resposta);
                }
            } catch (SocketException e) {
                // Captura desconexão abrupta pelo cliente (ex: envio de pacote TCP RST ou encerramento do processo cliente)
                System.out.println("[REDE] Conexão resetada pelo cliente da porta " + socket.getPort() + ".");
            } catch (IOException e) {
                // Captura erros gerais de leitura e escrita na camada de transporte
                System.err.println("[REDE] Erro de E/S na comunicação com o cliente: " + e.getMessage());
            } finally {
                // Encerra graciosamente os fluxos e o socket do cliente
                try {
                    // Fecha o fluxo de entrada e libera os recursos associados
                    if (entrada != null) entrada.close();
                    // Fecha o fluxo de saída e libera os recursos associados
                    if (saida != null) saida.close();
                    // Invoca close() no socket cliente, enviando o pacote TCP FIN para finalizar a conexão de rede
                    if (socket != null && !socket.isClosed()) socket.close();
                    // Notifica no console que os descritores e a conexão com o cliente foram liberados
                    System.out.println("[DESCONECTADO] Conexão com a porta " + socket.getPort() + " fechada.");
                } catch (IOException e) {
                    // Trata falha no encerramento do socket
                    e.printStackTrace();
                }
            }
        }

        /**
         * Interpreta o protocolo de aplicação e calcula os resultados.
         * Formatos aceitos:
         *   - OP <operador> <num1> <num2>    (Operação individual - usado na Abordagem 1)
         *   - EXPR <expressão matemática>   (Expressão inteira - Abordagem 2)
         */
        private String processarComando(String comando) {
            String trimmed = comando.trim();
            if (trimmed.isEmpty()) {
                return "ERROR Comando vazio.";
            }

            String[] partes = trimmed.split("\\s+", 4);
            String tipo = partes[0].toUpperCase();

            try {
                if (tipo.equals("OP")) {
                    if (partes.length < 4) {
                        return "ERROR Formato inválido. Use: OP <operador> <num1> <num2>";
                    }
                    String op = partes[1];
                    double a = Double.parseDouble(partes[2]);
                    double b = Double.parseDouble(partes[3]);
                    double resultado;

                    switch (op) {
                        case "+": resultado = a + b; break;
                        case "-": resultado = a - b; break;
                        case "*": resultado = a * b; break;
                        case "/":
                            if (b == 0) return "ERROR Divisão por zero.";
                            resultado = a / b;
                            break;
                        default:
                            return "ERROR Operador desconhecido: " + op;
                    }
                    return "OK " + resultado;

                } else if (tipo.equals("EXPR")) {
                    if (partes.length < 2) {
                        return "ERROR Formato inválido. Use: EXPR <expressao>";
                    }
                    // Recupera a expressão completa (tudo após a palavra EXPR)
                    String expressao = trimmed.substring(4).trim();
                    // Avalia a expressão no servidor utilizando o parser local (sem callback de rede)
                    double resultado = ExpressionEvaluator.evaluate(expressao, null);
                    return "OK " + resultado;

                } else {
                    return "ERROR Comando desconhecido: " + tipo;
                }
            } catch (ArithmeticException ae) {
                return "ERROR " + ae.getMessage();
            } catch (Exception ex) {
                return "ERROR Erro ao processar expressão: " + ex.getMessage();
            }
        }
    }
}
