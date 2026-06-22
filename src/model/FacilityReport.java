package asrama.model;

import asrama.enums.ReportStatus;
import java.time.LocalDateTime;

public class FacilityReport {
    private final String reportId;
    private final String deskripsi;
    private final String lokasi;
    private final LocalDateTime tanggalLapor;
    private ReportStatus status;
    private String mahasiswaId;

    public FacilityReport(String reportId, String deskripsi, String lokasi) {
        this(reportId, deskripsi, lokasi, LocalDateTime.now(), ReportStatus.BARU);
    }

    public FacilityReport(String reportId, String deskripsi, String lokasi,
            LocalDateTime tanggalLapor, ReportStatus status) {
        this.reportId = reportId;
        this.deskripsi = deskripsi;
        this.lokasi = lokasi;
        this.tanggalLapor = tanggalLapor;
        this.status = status;
    }

    public void ubahStatus(ReportStatus status) {
        this.status = status;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public String getDeskripsi() {
        return deskripsi;
    }

    public String getReportId() {
        return reportId;
    }

    public String getLokasi() {
        return lokasi;
    }

    public LocalDateTime getTanggalLapor() {
        return tanggalLapor;
    }

    public String getMahasiswaId() {
        return mahasiswaId;
    }

    public void setMahasiswaId(String mahasiswaId) {
        this.mahasiswaId = mahasiswaId;
    }
}
