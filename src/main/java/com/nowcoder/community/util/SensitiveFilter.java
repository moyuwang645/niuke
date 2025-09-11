package com.nowcoder.community.util;

import org.apache.commons.lang3.CharUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;


@Component
public class SensitiveFilter {
    private static Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);
    private static final String Replace="***";
    private TireNode root = new TireNode();

    @PostConstruct
    public void init() {
        try(InputStream is=this.getClass().getClassLoader().getResourceAsStream("banword.txt");
            BufferedReader br=new BufferedReader(new InputStreamReader(is));){
            String keyword;
            while((keyword=br.readLine())!=null){
                this.addkeyword(keyword);
            }
        }
        catch(IOException e){
            logger.error("加载错误"+e.getMessage());
        }

    }

    private void addkeyword(String keyword){
        TireNode node=root;
        for(int i=0;i<keyword.length();i++){
            char c=keyword.charAt(i);
            TireNode subnode=node.getSubnode(c);
            if(subnode==null){
                subnode=new TireNode();
                node.addSubnode(subnode,c);
            }
            node=subnode;
            if(i==keyword.length()-1){
                node.setEndnode(true);
            }
        }
    }

    public String filter(String keyword){
        TireNode temp=root;
        int begin=0;
        int end=0;
        StringBuilder sb=new StringBuilder();
        while(end<keyword.length()){
            char c=keyword.charAt(end);
            if(isSymbol(c)){
                if(temp==root){
                    sb.append(c);
                    begin++;
                }else{
                    end++;
                    continue;
                }
            }
            temp =temp.getSubnode(c);
            if(temp==null){
                sb.append(c);
                end=++begin;
                temp=root;
            } else if (temp.isEndnode()) {
                sb.append(Replace);
                begin=++end;
                temp=root;
            }else {
                end++;
            }
        }
        sb.append(keyword.substring(begin));
        return sb.toString();
    }
    private boolean isSymbol(Character ch){
        return !CharUtils.isAsciiAlphanumeric(ch)&&(ch<0x2E80||ch>0x9FFF);
    }

    private class TireNode{
        private boolean endnode=false;
        private Map<Character,TireNode> subnodes=new HashMap<>();
        public boolean isEndnode() {
            return this.endnode;
        }

        public void setEndnode(boolean endnode) {
            this.endnode = endnode;
        }

        public void addSubnode(TireNode subnode,Character key) {
            subnodes.put(key,subnode);
        }
        public TireNode getSubnode(Character key) {
            return subnodes.get(key);
        }
    }
}
