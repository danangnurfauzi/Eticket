package com.example.danangnurfauzi.eticket;

/**
 * Created by danangnurfauzi on 7/18/17.
 */

public class RegoTiket {
    private int id;
    private int wisatawanNusantara;
    private int wisatawanMancanegara;
    private int kendaraanYa;
    private int kendaraanTidak;

    public RegoTiket(){

    }

    public RegoTiket(int wisatawanNusantara, int wisatawanMancanegara, int kendaraanYa, int kendaraanTidak){
        super();
        this.wisatawanNusantara     = wisatawanNusantara;
        this.wisatawanMancanegara   = wisatawanMancanegara;
        this.kendaraanYa            = kendaraanYa;
        this.kendaraanTidak         = kendaraanTidak;
    }

    public int getId(){
        return id;
    }

    public void setId(int id){
        this.id = id;
    }

    public void setWisatawanNusantara(int wisatawanNusantara){
        this.wisatawanNusantara = wisatawanNusantara;
    }

    public void setWisatawanMancanegara(int wisatawanMancanegara){
        this.wisatawanMancanegara = wisatawanMancanegara;
    }

    public void setKendaraanYa(int kendaraanYa){
        this.kendaraanYa = kendaraanYa;
    }

    public void setKendaraanTidak(int kendaraanTidak){
        this.kendaraanTidak = kendaraanTidak;
    }

    public int getWisatawanNusantara(){
        return wisatawanNusantara;
    }

    public int getWisatawanMancanegara(){
        return wisatawanMancanegara;
    }

    public int getKendaraanYa(){
        return kendaraanYa;
    }

    public int getKendaraanTidak(){
        return kendaraanTidak;
    }

}
