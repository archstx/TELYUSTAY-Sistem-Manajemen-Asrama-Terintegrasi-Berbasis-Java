package asrama.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Gedung {
    private final String kodeGedung;
    private final String namaGedung;
    private String jenisGedung;
    private final List<Kamar> daftarKamar;

    public Gedung(String kodeGedung, String namaGedung) {
        this.kodeGedung = kodeGedung;
        this.namaGedung = namaGedung;
        this.daftarKamar = new ArrayList<>();
    }

    public void tambahKamar(Kamar kamar) {
        if (!daftarKamar.contains(kamar)) {
            daftarKamar.add(kamar);
        }
    }

    public void hapusKamar(Kamar kamar) {
        daftarKamar.remove(kamar);
    }

    public List<Kamar> getKamarTersedia() {
        List<Kamar> tersedia = new ArrayList<>();
        for (Kamar kamar : daftarKamar) {
            if (kamar.cekKetersediaan()) {
                tersedia.add(kamar);
            }
        }
        return tersedia;
    }

    public int hitungTotalPenghuni() {
        int total = 0;
        for (Kamar kamar : daftarKamar) {
            total += kamar.getJumlahPenghuni();
        }
        return total;
    }

    public String getKodeGedung() {
        return kodeGedung;
    }

    public String getNamaGedung() {
        return namaGedung;
    }

    public String getJenisGedung() {
        return jenisGedung;
    }

    public void setJenisGedung(String jenisGedung) {
        this.jenisGedung = jenisGedung;
    }

    public List<Kamar> getDaftarKamar() {
        return Collections.unmodifiableList(daftarKamar);
    }
}
