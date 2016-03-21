package linker;

import java.io.IOException;
import java.util.StringTokenizer;

import mvn.util.LinkerSymbolTable;

/**
 * Passo 2 do ligador.<br>
 * Nesse passo é gerado o código objeto a partir da tabela
 * de símbolos obtida.
 * @author FLevy
 * @version 23.10.2006
 * Preparação do arquivo para alunos - PSMuniz 1.11.2006
 * @version 01.01.2010 : atualização da classe de acordo com a definição dos slides. (Tiago)
 */
public class Pass2 extends Pass {

    /**Gerencia o arquivo de saída*/
    private Output out;
    /**Tabela de símbolos utilizada pelo Linker*/
    private LinkerSymbolTable symbolTable;
    /**Indica o endereçamento corrente da parte relocável do código. */
    private int relativeLocationCouter;
    /**A base de relocação a ser considerada no código. */
    private int base;

    /**Contador de variáveis externas que não foram resolvidas*/
    private int externalCounter = 0;


    public Pass2(LinkerSymbolTable symbolTable, String objFile) throws IOException {
        out = new Output(objFile);
        this.symbolTable = symbolTable;
        relativeLocationCouter = 0;
        base = 0;
    }

    /**
     * Processa uma linha de código.
     *
     * @param nibble O nibble do endereço da linha.
     * @param address O endereço da linha (sem o nibble).
     * @param code O código da linha.
     * @param currentFile O arquivo atual que está sendo processado.
     * @return Verdadeiro caso a análise teve sucesso, falso caso contrario.
     * @exception Caso tenha ocorrido algum problema de IO.
     */
    protected boolean processCode(int nibble, String address, String code, String currentFile)
            throws IOException {
        int endereco = Integer.parseInt(address,16);
        String codigo = code;
        boolean argrelocavel = false;
        if(isArgumentRelocable(nibble)){
            argrelocavel = true;
        }

        boolean relocavel = false;
        if(isRelocable(nibble)){
            endereco +=this.base;
            relocavel = true;
            this.relativeLocationCouter += 2;
        }

        int prim = Integer.parseInt(code.substring(1),16);
        String saida = this.symbolTable.getSymbol(currentFile,prim);
        boolean ehsaida = true;
        if(saida != null && saida.startsWith("5") && (nibble == 5 || nibble == 13)){
            ehsaida = false;
        }

        if((saida = (saida = "0000"+ saida).substring(saida.length() - 3, saida.length())) != null){
            if(nibble%2 == 1){
                LinkerSymbolTable tabela;
                argrelocavel = (tabela = this.symbolTable).isRelocable(tabela.getAddressByCode(currentFile,prim));
                codigo = code.substring(0,1) + saida;
            }

            if((nibble >>2)%2 != 0) {
                codigo = code.substring(0, 1) + saida;
            }
        } else if(nibble % 2 == 1){
            codigo = code;
        }

        if(isArgumentRelocable(nibble)){
            nibble = Integer.parseInt(endereco,16) + this.base;
            codigo = (codigo = "0000" + Integer.toHexString(nibble)).substring(endereco.length() -4, endereco.length());
        }

        this.out.write(endereco,codigo,relocavel,argrelocavel,ehsaida);
        return true;

    }//method

    protected boolean processSymbolicalAddress(int nibble, String address, String symbol, String currentFile, String originalLine){
        if(isEntryPoint(nibble)){
            this.out.writeExternal(Integer.toHexString(nibble), Integer.parseInt(this.symbolTable.getSymbolValue(symbol), 16), originalLine);
        } else {
            if(!this.symbolTable.symbolInTable(symbol)) {
                this.symbolTable.definedSymbol(symbol);
                String local = "0000" + Integer.toHexString(this.base);
                local = "5" + local.substring(local.length() - 3, local.length());
                this.symbolTable.setSymbolValue(symbol,local);
                this.out.writeExternal("4", Integer.parseInt(this.symbolTable.getSymbolValue(symbol), 16), originalLine);
                ++this.externalCounter;
            }

            StringTokenizer palavra = new StringTokenizer(originalLine);
            this.symbolTable.setCodeForSymbol(symbol, currentFile, Integer.parseInt(palavra.nextToken().substring(1, 4), 16));
        }
        return true;
    }

    /**
     * Finaliza o arquivo lido (pode haver um próximo arquivo).
     */
    protected void fileEnd() {
        this.base += this.relativeLocationCouter;
        this.relativeLocationCouter = 0;
    }

    public void closeOutput() throws IOException {
        out.close();
    }
}
