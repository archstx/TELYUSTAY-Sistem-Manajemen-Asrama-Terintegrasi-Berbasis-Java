package asrama.model;

import asrama.enums.PermitStatus;
import asrama.interfacepkg.PermitSystem;
import java.time.LocalDateTime;

public class IzinKeluar implements PermitSystem {
    private final String izinId;
    private final String tujuan;
    private LocalDateTime waktuKeluar;
    private LocalDateTime waktuKembali;
    private PermitStatus status;
    private String alasanPenolakan;
    private String mahasiswaId;

    public IzinKeluar(String izinId, String tujuan) {
        this.izinId = izinId;
        this.tujuan = tujuan;
        this.status = PermitStatus.DIAJUKAN;
    }

    @Override
    public boolean applyPermit(Mahasiswa mahasiswa) {
        if (!validateStatus(mahasiswa)) {
            this.status = PermitStatus.DITOLAK;
            this.alasanPenolakan = "Mahasiswa belum memiliki kamar.";
            return false;
        }

        if (!cekJamMalam()) {
            this.status = PermitStatus.DITOLAK;
            this.alasanPenolakan = "Waktu kembali melewati jam malam.";
            return false;
        }

        this.status = PermitStatus.DIAJUKAN;
        return true;
    }

    @Override
    public boolean validateStatus(Mahasiswa mahasiswa) {
        return mahasiswa != null && mahasiswa.getKamar() != null;
    }

    public boolean cekJamMalam() {
        if (waktuKeluar == null || waktuKembali == null) {
            return false;
        }
        return !waktuKembali.isAfter(waktuKembali.toLocalDate().atTime(22, 0));
    }

    public void setStatus(PermitStatus status) {
        this.status = status;
    }

    public PermitStatus getStatus() {
        return status;
    }

    public void setWaktuKeluar(LocalDateTime waktuKeluar) {
        this.waktuKeluar = waktuKeluar;
    }

    public void setWaktuKembali(LocalDateTime waktuKembali) {
        this.waktuKembali = waktuKembali;
    }

    public String getIzinId() {
        return izinId;
    }

    public String getTujuan() {
        return tujuan;
    }

    public LocalDateTime getWaktuKeluar() {
        return waktuKeluar;
    }

    public LocalDateTime getWaktuKembali() {
        return waktuKembali;
    }

    public String getAlasanPenolakan() {
        return alasanPenolakan;
    }

    public void setAlasanPenolakan(String alasanPenolakan) {
        this.alasanPenolakan = alasanPenolakan;
    }

    public String getMahasiswaId() {
        return mahasiswaId;
    }

    public void setMahasiswaId(String mahasiswaId) {
        this.mahasiswaId = mahasiswaId;
    }
}
