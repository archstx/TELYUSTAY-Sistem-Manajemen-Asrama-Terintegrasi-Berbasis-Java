package asrama.model;

import asrama.enums.PaymentStatus;
import java.time.LocalDate;

public class Payment {
    private final String paymentId;
    private final double jumlahTagihan;
    private double denda;
    private LocalDate tanggalBayar;
    private PaymentStatus status;
    private String mahasiswaId;

    public Payment(String paymentId, double jumlahTagihan) {
        this(paymentId, jumlahTagihan, 0, null, PaymentStatus.BELUM_BAYAR);
    }

    public Payment(String paymentId, double jumlahTagihan, double denda,
            LocalDate tanggalBayar, PaymentStatus status) {
        this.paymentId = paymentId;
        this.jumlahTagihan = jumlahTagihan;
        this.denda = denda;
        this.tanggalBayar = tanggalBayar;
        this.status = status;
    }

    public double hitungTotalBayar() {
        return jumlahTagihan + denda;
    }

    public boolean prosesPembayaran() {
        if (status == PaymentStatus.LUNAS) {
            return false;
        }
        this.tanggalBayar = LocalDate.now();
        this.status = PaymentStatus.LUNAS;
        return true;
    }

    public void tambahDenda(double nominal) {
        if (nominal > 0) {
            this.denda += nominal;
            if (status != PaymentStatus.LUNAS) {
                this.status = PaymentStatus.TERLAMBAT;
            }
        }
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public LocalDate getTanggalBayar() {
        return tanggalBayar;
    }

    public double getJumlahTagihan() {
        return jumlahTagihan;
    }

    public double getDenda() {
        return denda;
    }

    public String getMahasiswaId() {
        return mahasiswaId;
    }

    public void setMahasiswaId(String mahasiswaId) {
        this.mahasiswaId = mahasiswaId;
    }
}
