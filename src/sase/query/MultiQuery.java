package sase.query;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 解析多查询文件
 */
public class MultiQuery {
    /**
     * 多查询文件转变为nfas
     */
    List<NFA> nfas;

    /**
     * 查询文件位置
     */
    String multiQueryFile;

    public MultiQuery(String multiQueryFile){
        this.multiQueryFile = multiQueryFile;
        nfas = new LinkedList<>();
        parseMultiQueryFile();
    }

    /**
     * 解析多查询文件
     */
    public void parseMultiQueryFile(){
        String line = "";
        NFA nfa = null;
        try{
            BufferedReader br = new BufferedReader(new FileReader(this.multiQueryFile));
            while((line = br.readLine()) != null){
                //query之间是用“-----”隔开
                if(line.startsWith("-")){
                    if(nfa.hasMorePartitionAttribute){
                        nfa.addMorePartitionAttribute();
                    }
                    if(nfa.size > 0){
                        nfa.states[0].setStart(true);
                        nfa.states[nfa.size-1].setEnding(true);
                    }
                    nfa.testNegation();;
                    nfa.compileValueVectorOptimized();
                    nfas.add(nfa);
                }
                else{
                    //如果是一个查询的开始，则将初始化一个新的NFA
                    if(line.startsWith("PATTERN")){
                        nfa = new NFA();
                        nfa.morePartitionAttribute = new ArrayList<String>();
                        nfa.hasMorePartitionAttribute = false;
                    }
                    nfa.parseFastQueryLine(line);
                }
            }

        } catch (FileNotFoundException e){
            // TODO: 2020/7/19 do something for file not found
            e.printStackTrace();
        } catch (IOException e){
            // TODO: 2020/7/19 do something for IOException
            e.printStackTrace();
        }
    }

    public List<NFA> getNfas(){
        return this.nfas;
    }

    @Override
    public String toString() {
        String temp = "";
        for(int i = 0; i < this.nfas.size(); i++){
            temp += this.nfas.get(i).toString() + "\n\n";
        }
        return temp;
    }


}
