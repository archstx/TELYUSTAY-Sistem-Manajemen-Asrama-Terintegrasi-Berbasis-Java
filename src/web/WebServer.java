package asrama.web;

import asrama.database.DatabaseConnection;
import asrama.enums.ReportStatus;
import asrama.model.FacilityReport;
import asrama.model.Gedung;
import asrama.model.IzinKeluar;
import asrama.model.Kamar;
import asrama.model.Mahasiswa;
import asrama.model.Payment;
import asrama.service.AsramaCrudService;
import asrama.ai.AsramaAiAssistant;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WebServer {
    private static final int PORT_AWAL = 8080;
    private static final int PORT_AKHIR = 8090;
    private int portAktif = PORT_AWAL;
    private final AsramaCrudService crud = new AsramaCrudService();
    private final Path webRoot;

    public WebServer() throws IOException {
        this.webRoot = resolveWebRoot();
    }

    private static Path resolveWebRoot() throws IOException {
        Path userDir = Path.of(System.getProperty("user.dir"));
        Path[] candidates = {
                userDir.resolve("web"),
                userDir.resolve("..").resolve("web").normalize(),
                Path.of("web")
        };

        for (Path candidate : candidates) {
            Path index = candidate.resolve("index.html");
            if (Files.isRegularFile(index)) {
                return candidate.toAbsolutePath().normalize();
            }
        }

        throw new IOException(
                "Folder web tidak ditemukan. Pastikan ada web/index.html di root project.");
    }

    public void start() throws IOException {
        if (!DatabaseConnection.testConnection()) {
            throw new IOException("Koneksi database gagal. Pastikan MySQL aktif dan file database_asrama.sql sudah diimport.");
        }

        HttpServer server = null;
        IOException lastError = null;
        for (int port = PORT_AWAL; port <= PORT_AKHIR; port++) {
            try {
                server = HttpServer.create(new InetSocketAddress(port), 0);
                portAktif = port;
                break;
            } catch (IOException ex) {
                lastError = ex;
            }
        }

        if (server == null) {
            throw new IOException(
                    "Port " + PORT_AWAL + "-" + PORT_AKHIR + " sudah dipakai. Tutup instance TelyuStay/Java lain atau hentikan proses di port tersebut.",
                    lastError);
        }

        server.createContext("/", new StaticHandler());
        server.createContext("/api/stats", new StatsHandler());
        server.createContext("/api/mahasiswa", new MahasiswaHandler());
        server.createContext("/api/gedung", new GedungHandler());
        server.createContext("/api/kamar", new KamarHandler());
        server.createContext("/api/izin", new IzinHandler());
        server.createContext("/api/laporan", new LaporanHandler());
        server.createContext("/api/payment", new PaymentHandler());
        server.createContext("/api/ai/chat", new AiChatHandler());
        server.setExecutor(null);
        server.start();

        System.out.println("========================================");
        System.out.println("  TelyuStay Web Server berjalan!");
        if (portAktif != PORT_AWAL) {
            System.out.println("  Port " + PORT_AWAL + " sedang dipakai, menggunakan port " + portAktif);
        }
        System.out.println("  Folder web : " + webRoot);
        System.out.println("  Buka       : http://localhost:" + portAktif);
        System.out.println("========================================");
    }

    public int getPortAktif() {
        return portAktif;
    }

    public String getUrl() {
        return "http://localhost:" + portAktif;
    }

    private void addCors(Headers headers) {
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type");
    }

    private void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        addCors(headers);
        headers.add("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendText(HttpExchange exchange, int status, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        addCors(headers);
        headers.add("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                addCors(exchange.getResponseHeaders());
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            if ("/".equals(path)) {
                path = "/index.html";
            }

            Path file = webRoot.resolve(path.substring(1)).normalize();
            if (!file.startsWith(webRoot) || !Files.exists(file) || Files.isDirectory(file)) {
                sendText(exchange, 404, "File tidak ditemukan");
                return;
            }

            String contentType = contentType(file.getFileName().toString());
            byte[] bytes = Files.readAllBytes(file);
            Headers headers = exchange.getResponseHeaders();
            addCors(headers);
            headers.add("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private String contentType(String filename) {
            if (filename.endsWith(".html")) {
                return "text/html; charset=UTF-8";
            }
            if (filename.endsWith(".css")) {
                return "text/css; charset=UTF-8";
            }
            if (filename.endsWith(".js")) {
                return "application/javascript; charset=UTF-8";
            }
            return "application/octet-stream";
        }
    }

    private class StatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                addCors(exchange.getResponseHeaders());
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            Map<String, Integer> stats = crud.getStats();
            String json = "{\"mahasiswa\":" + stats.get("mahasiswa")
                    + ",\"gedung\":" + stats.get("gedung")
                    + ",\"kamar\":" + stats.get("kamar")
                    + ",\"izin\":" + stats.get("izin")
                    + ",\"laporan\":" + stats.get("laporan")
                    + ",\"payment\":" + stats.get("payment") + "}";
            sendJson(exchange, 200, json);
        }
    }

    private class MahasiswaHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            handleCrud(exchange, "mahasiswa");
        }
    }

    private class GedungHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            handleCrud(exchange, "gedung");
        }
    }

    private class KamarHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            handleCrud(exchange, "kamar");
        }
    }

    private class IzinHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            handleCrud(exchange, "izin");
        }
    }

    private class LaporanHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            handleCrud(exchange, "laporan");
        }
    }

    private class PaymentHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            handleCrud(exchange, "payment");
        }
    }

    private class AiChatHandler implements HttpHandler {
        private final AsramaAiAssistant assistant = new AsramaAiAssistant();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if ("OPTIONS".equalsIgnoreCase(method)) {
                addCors(exchange.getResponseHeaders());
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(method)) {
                sendText(exchange, 405, "Method tidak didukung");
                return;
            }

            try {
                Map<String, String> data = JsonHelper.parseObject(readBody(exchange));
                String prompt = JsonHelper.get(data, "prompt");
                String jawaban = assistant.prosesPrompt(prompt);
                Map<String, String> response = new LinkedHashMap<>();
                response.put("response", jawaban);
                sendJson(exchange, 200, JsonHelper.object(response));
            } catch (RuntimeException ex) {
                sendJson(exchange, 500, "{\"response\":\"" + JsonHelper.escape(ex.getMessage()) + "\"}");
            }
        }
    }

    private void handleCrud(HttpExchange exchange, String entity) throws IOException {
        String method = exchange.getRequestMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            addCors(exchange.getResponseHeaders());
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            switch (method) {
                case "GET" -> sendJson(exchange, 200, readAllJson(entity));
                case "POST" -> {
                    Map<String, String> data = JsonHelper.parseObject(readBody(exchange));
                    createEntity(entity, data);
                    sendJson(exchange, 201, "{\"success\":true,\"message\":\"Data berhasil ditambahkan\"}");
                }
                case "PUT" -> {
                    Map<String, String> data = JsonHelper.parseObject(readBody(exchange));
                    updateEntity(entity, data);
                    sendJson(exchange, 200, "{\"success\":true,\"message\":\"Data berhasil diperbarui\"}");
                }
                case "DELETE" -> {
                    Map<String, String> data = JsonHelper.parseObject(readBody(exchange));
                    deleteEntity(entity, data);
                    sendJson(exchange, 200, "{\"success\":true,\"message\":\"Data berhasil dihapus\"}");
                }
                default -> sendText(exchange, 405, "Method tidak didukung");
            }
        } catch (RuntimeException ex) {
            sendJson(exchange, 500, "{\"success\":false,\"message\":\"" + JsonHelper.escape(ex.getMessage()) + "\"}");
        }
    }

    private String readAllJson(String entity) {
        return switch (entity) {
            case "mahasiswa" -> mahasiswaListJson();
            case "gedung" -> gedungListJson();
            case "kamar" -> kamarListJson();
            case "izin" -> izinListJson();
            case "laporan" -> laporanListJson();
            case "payment" -> paymentListJson();
            default -> "[]";
        };
    }

    private void createEntity(String entity, Map<String, String> data) {
        switch (entity) {
            case "mahasiswa" -> {
                String nomorKamar = JsonHelper.get(data, "nomorKamar");
                String kodeGedung = JsonHelper.get(data, "kodeGedung");
                Mahasiswa mhs = new Mahasiswa(
                        JsonHelper.get(data, "id"),
                        JsonHelper.get(data, "nama"),
                        JsonHelper.get(data, "nim"));
                mhs.setProdi(JsonHelper.get(data, "prodi"));
                mhs.setNoTelepon(JsonHelper.get(data, "noTelepon"));
                mhs.setCredentials(JsonHelper.get(data, "username"), JsonHelper.get(data, "password"));
                crud.createMahasiswa(
                        mhs,
                        nomorKamar.isEmpty() ? null : nomorKamar,
                        kodeGedung.isEmpty() ? null : kodeGedung);
            }
            case "gedung" -> {
                Gedung gedung = new Gedung(data.get("kodeGedung"), data.get("namaGedung"));
                gedung.setJenisGedung(data.get("jenisGedung"));
                crud.createGedung(gedung);
            }
            case "kamar" -> {
                Kamar kamar = new Kamar(data.get("nomorKamar"), Integer.parseInt(data.get("kapasitas")));
                crud.createKamar(kamar, data.get("kodeGedung"));
            }
            case "izin" -> {
                Mahasiswa mhs = crud.readMahasiswa(data.get("mahasiswaId"));
                if (mhs == null) {
                    throw new RuntimeException("Mahasiswa tidak ditemukan");
                }
                int jamKeluar = Integer.parseInt(data.getOrDefault("jamKeluar", "1"));
                int jamKembali = Integer.parseInt(data.getOrDefault("jamKembali", "3"));
                IzinKeluar izin = mhs.ajukanIzin(
                        data.get("tujuan"),
                        LocalDateTime.now().plusHours(jamKeluar),
                        LocalDateTime.now().plusHours(jamKembali)
                );
                crud.createIzin(izin);
            }
            case "laporan" -> {
                Mahasiswa mhs = crud.readMahasiswa(data.get("mahasiswaId"));
                if (mhs == null) {
                    throw new RuntimeException("Mahasiswa tidak ditemukan");
                }
                FacilityReport report = mhs.buatLaporan(data.get("deskripsi"), data.get("lokasi"));
                crud.createReport(report);
            }
            case "payment" -> {
                String paymentId = data.get("paymentId");
                if (paymentId.isEmpty()) {
                    paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8);
                }
                Payment payment = new Payment(paymentId, Double.parseDouble(data.get("jumlahTagihan")));
                payment.setMahasiswaId(data.get("mahasiswaId"));
                crud.createPayment(payment);
            }
            default -> throw new RuntimeException("Entitas tidak dikenal");
        }
    }

    private void updateEntity(String entity, Map<String, String> data) {
        switch (entity) {
            case "mahasiswa" -> {
                String nomorKamar = JsonHelper.get(data, "nomorKamar");
                String kodeGedung = JsonHelper.get(data, "kodeGedung");
                if (!crud.updateMahasiswa(
                        JsonHelper.get(data, "id"),
                        JsonHelper.get(data, "nama"),
                        JsonHelper.get(data, "nim"),
                        JsonHelper.get(data, "prodi"),
                        JsonHelper.get(data, "noTelepon"),
                        JsonHelper.get(data, "username"),
                        JsonHelper.get(data, "password"),
                        nomorKamar.isEmpty() ? null : nomorKamar,
                        kodeGedung.isEmpty() ? null : kodeGedung)) {
                    throw new RuntimeException("Data mahasiswa tidak ditemukan");
                }
            }
            case "gedung" -> crud.updateGedung(data.get("kodeGedung"), data.get("jenisGedung"));
            case "kamar" -> {
                Mahasiswa mhs = crud.readMahasiswa(data.get("mahasiswaId"));
                boolean tambah = "true".equalsIgnoreCase(data.get("tambahPenghuni"));
                crud.updateKamar(data.get("nomorKamar"), mhs, tambah);
            }
            case "izin" -> {
                Mahasiswa mhs = crud.readMahasiswa(data.get("mahasiswaId"));
                crud.updateIzin(data.get("izinId"), mhs);
            }
            case "laporan" -> crud.updateReport(data.get("reportId"), ReportStatus.valueOf(data.get("status")));
            case "payment" -> crud.updatePayment(
                    data.get("paymentId"),
                    Double.parseDouble(data.getOrDefault("denda", "0")),
                    "true".equalsIgnoreCase(data.get("prosesBayar")));
            default -> throw new RuntimeException("Entitas tidak dikenal");
        }
    }

    private void deleteEntity(String entity, Map<String, String> data) {
        boolean success = switch (entity) {
            case "mahasiswa" -> crud.deleteMahasiswa(data.get("id"));
            case "gedung" -> crud.deleteGedung(data.get("kodeGedung"));
            case "kamar" -> crud.deleteKamar(data.get("nomorKamar"));
            case "izin" -> crud.deleteIzin(data.get("izinId"));
            case "laporan" -> crud.deleteReport(data.get("reportId"));
            case "payment" -> crud.deletePayment(data.get("paymentId"));
            default -> throw new RuntimeException("Entitas tidak dikenal");
        };
        if (!success) {
            throw new RuntimeException("Data tidak ditemukan");
        }
    }

    private String mahasiswaListJson() {
        List<Mahasiswa> list = crud.readAllMahasiswa();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            Mahasiswa mhs = list.get(i);
            if (i > 0) {
                sb.append(",");
            }
            Map<String, String> map = new LinkedHashMap<>();
            map.put("id", mhs.getId());
            map.put("nama", mhs.getNama());
            map.put("nim", mhs.getNim());
            map.put("prodi", mhs.getProdi() == null ? "" : mhs.getProdi());
            map.put("noTelepon", mhs.getNoTelepon() == null ? "" : mhs.getNoTelepon());
            map.put("username", mhs.getUsername() == null ? "" : mhs.getUsername());
            map.put("nomorKamar", mhs.getKamar() == null ? "" : mhs.getKamar().getNomorKamar());
            map.put("kodeGedung", mhs.getKamar() == null || mhs.getKamar().getKodeGedung() == null
                    ? "" : mhs.getKamar().getKodeGedung());
            map.put("namaGedung", mhs.getKamar() == null || mhs.getKamar().getNamaGedung() == null
                    ? "" : mhs.getKamar().getNamaGedung());
            sb.append(JsonHelper.object(map));
        }
        sb.append("]");
        return sb.toString();
    }

    private String gedungListJson() {
        List<Gedung> list = crud.readAllGedung();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            Gedung g = list.get(i);
            if (i > 0) {
                sb.append(",");
            }
            Map<String, String> map = new LinkedHashMap<>();
            map.put("kodeGedung", g.getKodeGedung());
            map.put("namaGedung", g.getNamaGedung());
            map.put("jenisGedung", g.getJenisGedung() == null ? "" : g.getJenisGedung());
            map.put("totalKamar", String.valueOf(g.getDaftarKamar().size()));
            map.put("totalPenghuni", String.valueOf(g.hitungTotalPenghuni()));
            sb.append(JsonHelper.object(map));
        }
        sb.append("]");
        return sb.toString();
    }

    private String kamarListJson() {
        List<Kamar> list = crud.readAllKamar();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            Kamar k = list.get(i);
            if (i > 0) {
                sb.append(",");
            }
            Map<String, String> map = new LinkedHashMap<>();
            map.put("nomorKamar", k.getNomorKamar());
            map.put("kodeGedung", k.getKodeGedung() == null ? "" : k.getKodeGedung());
            map.put("namaGedung", k.getNamaGedung() == null ? "" : k.getNamaGedung());
            map.put("kapasitas", String.valueOf(k.getKapasitas()));
            map.put("jumlahPenghuni", String.valueOf(k.getJumlahPenghuni()));
            map.put("sisaSlot", String.valueOf(k.getSisaSlot()));
            map.put("penghuni", crud.getNamaPenghuniKamar(k.getNomorKamar()));
            sb.append(JsonHelper.object(map));
        }
        sb.append("]");
        return sb.toString();
    }

    private String izinListJson() {
        List<IzinKeluar> list = crud.readAllIzin();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            IzinKeluar izin = list.get(i);
            if (i > 0) {
                sb.append(",");
            }
            Map<String, String> map = new LinkedHashMap<>();
            map.put("izinId", izin.getIzinId());
            map.put("mahasiswaId", izin.getMahasiswaId() == null ? "" : izin.getMahasiswaId());
            map.put("tujuan", izin.getTujuan());
            map.put("status", izin.getStatus().name());
            map.put("alasan", izin.getAlasanPenolakan() == null ? "" : izin.getAlasanPenolakan());
            sb.append(JsonHelper.object(map));
        }
        sb.append("]");
        return sb.toString();
    }

    private String laporanListJson() {
        List<FacilityReport> list = crud.readAllReport();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            FacilityReport report = list.get(i);
            if (i > 0) {
                sb.append(",");
            }
            Map<String, String> map = new LinkedHashMap<>();
            map.put("reportId", report.getReportId());
            map.put("mahasiswaId", report.getMahasiswaId() == null ? "" : report.getMahasiswaId());
            map.put("deskripsi", report.getDeskripsi());
            map.put("lokasi", report.getLokasi());
            map.put("status", report.getStatus().name());
            sb.append(JsonHelper.object(map));
        }
        sb.append("]");
        return sb.toString();
    }

    private String paymentListJson() {
        List<Payment> list = crud.readAllPayment();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            Payment payment = list.get(i);
            if (i > 0) {
                sb.append(",");
            }
            Map<String, String> map = new LinkedHashMap<>();
            map.put("paymentId", payment.getPaymentId());
            map.put("mahasiswaId", payment.getMahasiswaId() == null ? "" : payment.getMahasiswaId());
            map.put("jumlahTagihan", String.valueOf((long) payment.getJumlahTagihan()));
            map.put("denda", String.valueOf((long) payment.getDenda()));
            map.put("total", String.valueOf((long) payment.hitungTotalBayar()));
            map.put("status", payment.getStatus().name());
            sb.append(JsonHelper.object(map));
        }
        sb.append("]");
        return sb.toString();
    }
}
