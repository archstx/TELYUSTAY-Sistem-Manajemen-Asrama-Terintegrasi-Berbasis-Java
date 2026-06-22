package asrama.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ActivityLogger {
    private final String logId;
    private final LocalDateTime waktuAktivitas;
    private final String aktivitas;
    private final String keterangan;
    private final User user;
    private static final List<ActivityLogger> LOGS = new ArrayList<>();

    public ActivityLogger(String logId, String aktivitas) {
        this(logId, aktivitas, "-", null);
    }

    public ActivityLogger(String logId, String aktivitas, String keterangan, User user) {
        this.logId = logId;
        this.aktivitas = aktivitas;
        this.keterangan = keterangan;
        this.user = user;
        this.waktuAktivitas = LocalDateTime.now();
    }

    public void catatLog(User user, String aktivitas) {
        ActivityLogger logger = new ActivityLogger("LOG-" + UUID.randomUUID(), aktivitas, "-", user);
        LOGS.add(logger);
    }

    public List<ActivityLogger> getLogByUser(User user) {
        List<ActivityLogger> result = new ArrayList<>();
        for (ActivityLogger logger : LOGS) {
            if (logger.user != null && logger.user.getId().equals(user.getId())) {
                result.add(logger);
            }
        }
        return result;
    }

    public void cetakLog() {
        for (ActivityLogger logger : LOGS) {
            System.out.println("[" + logger.waktuAktivitas + "] "
                    + (logger.user == null ? "SYSTEM" : logger.user.getNama())
                    + " -> " + logger.aktivitas);
        }
    }

    public String getLogId() {
        return logId;
    }

    public LocalDateTime getWaktuAktivitas() {
        return waktuAktivitas;
    }

    public String getAktivitas() {
        return aktivitas;
    }

    public String getKeterangan() {
        return keterangan;
    }
}
