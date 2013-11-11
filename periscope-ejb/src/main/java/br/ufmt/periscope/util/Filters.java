package br.ufmt.periscope.util;

import java.util.Date;

public class Filters {
    
    private boolean complete;
    private int selecionaData;
    private Date inicio, fim;

    
    public Filters() {
    }
    
    

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public int getSelecionaData() {
        return selecionaData;
    }

    public void setSelecionaData(int selecionaData) {
        this.selecionaData = selecionaData;
    }

    public Date getInicio() {
        return inicio;
    }

    public void setInicio(Date inicio) {
        this.inicio = inicio;
    }

    public Date getFim() {
        return fim;
    }

    public void setFim(Date fim) {
        this.fim = fim;
    }
    
    
}
