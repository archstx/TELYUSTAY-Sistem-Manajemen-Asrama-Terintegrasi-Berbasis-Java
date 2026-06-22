package asrama.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Kamar {
    private final String nomorKamar;
    private final int kapasitas;
    private int jumlahPenghuni;
    private final List<Mahasiswa> penghuni;
    private String kodeGedung;
    private String namaGedung;

    public Kamar(String nomorKamar, int kapasitas) {
        this.nomorKamar = nomorKamar;
        this.kapasitas = kapasitas;
        this.penghuni = new ArrayList<>();
    }

    public boolean tambahPenghuni(Mahasiswa mahasiswa) {
        if (!cekKetersediaan() || penghuni.contains(mahasiswa)) {
            return false;
        }
        penghuni.add(mahasiswa);
        jumlahPenghuni = penghuni.size();
        mahasiswa.setKamar(this);
        return true;
    }

    public boolean hapusPenghuni(Mahasiswa mahasiswa) {
        boolean removed = penghuni.remove(mahasiswa);
        if (removed) {
            jumlahPenghuni = penghuni.size();
            mahasiswa.setKamar(null);
        }
        return removed;
    }

    public boolean cekKetersediaan() {
        return jumlahPenghuni < kapasitas;
    }

    public int getSisaSlot() {
        return kapasitas - jumlahPenghuni;
    }

    public String getNomorKamar() {
        return nomorKamar;
    }

    public int getKapasitas() {
        return kapasitas;
    }

    public int getJumlahPenghuni() {
        return jumlahPenghuni;
    }

    public void sinkronJumlahPenghuni(int jumlah) {
        this.jumlahPenghuni = jumlah;
    }

    public String getKodeGedung() {
        return kodeGedung;
    }

    public void setKodeGedung(String kodeGedung) {
        this.kodeGedung = kodeGedung;
    }

    public String getNamaGedung() {
        return namaGedung;
    }

    public void setNamaGedung(String namaGedung) {
        this.namaGedung = namaGedung;
    }

    public List<Mahasiswa> getPenghuni() {
        return Collections.unmodifiableList(penghuni);
    }
}
