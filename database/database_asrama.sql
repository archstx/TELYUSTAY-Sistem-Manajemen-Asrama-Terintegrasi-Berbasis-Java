-- Database: database_asrama
-- Sistem Manajemen Asrama TelyuStay

CREATE DATABASE IF NOT EXISTS database_asrama;
USE database_asrama;

DROP TABLE IF EXISTS activity_log;
DROP TABLE IF EXISTS payment;
DROP TABLE IF EXISTS facility_report;
DROP TABLE IF EXISTS izin_keluar;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS kamar;
DROP TABLE IF EXISTS gedung;

CREATE TABLE gedung (
    kode_gedung VARCHAR(10) PRIMARY KEY,
    nama_gedung VARCHAR(100) NOT NULL,
    jenis_gedung VARCHAR(50)
);

CREATE TABLE kamar (
    nomor_kamar VARCHAR(10) PRIMARY KEY,
    kapasitas INT NOT NULL,
    kode_gedung VARCHAR(10),
    CONSTRAINT fk_kamar_gedung FOREIGN KEY (kode_gedung)
        REFERENCES gedung(kode_gedung) ON DELETE SET NULL
);

CREATE TABLE users (
    id VARCHAR(20) PRIMARY KEY,
    nama VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL,
    username VARCHAR(50),
    password VARCHAR(100),
    nim VARCHAR(20),
    prodi VARCHAR(50),
    no_telepon VARCHAR(20),
    nomor_sr VARCHAR(20),
    nomor_pegawai VARCHAR(20),
    nomor_kamar VARCHAR(10),
    kode_gedung_tugas VARCHAR(10),
    CONSTRAINT fk_user_kamar FOREIGN KEY (nomor_kamar)
        REFERENCES kamar(nomor_kamar) ON DELETE SET NULL,
    CONSTRAINT fk_user_gedung FOREIGN KEY (kode_gedung_tugas)
        REFERENCES gedung(kode_gedung) ON DELETE SET NULL
);

CREATE TABLE izin_keluar (
    izin_id VARCHAR(50) PRIMARY KEY,
    mahasiswa_id VARCHAR(20) NOT NULL,
    tujuan VARCHAR(200) NOT NULL,
    waktu_keluar DATETIME,
    waktu_kembali DATETIME,
    status VARCHAR(20) NOT NULL,
    alasan_penolakan VARCHAR(255),
    CONSTRAINT fk_izin_mahasiswa FOREIGN KEY (mahasiswa_id)
        REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE facility_report (
    report_id VARCHAR(50) PRIMARY KEY,
    mahasiswa_id VARCHAR(20) NOT NULL,
    deskripsi TEXT NOT NULL,
    lokasi VARCHAR(100) NOT NULL,
    tanggal_lapor DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT fk_report_mahasiswa FOREIGN KEY (mahasiswa_id)
        REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE payment (
    payment_id VARCHAR(50) PRIMARY KEY,
    mahasiswa_id VARCHAR(20),
    jumlah_tagihan DECIMAL(12, 2) NOT NULL,
    denda DECIMAL(12, 2) DEFAULT 0,
    tanggal_bayar DATE,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT fk_payment_mahasiswa FOREIGN KEY (mahasiswa_id)
        REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE activity_log (
    log_id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(20),
    aktivitas VARCHAR(200) NOT NULL,
    keterangan VARCHAR(255),
    waktu_aktivitas DATETIME NOT NULL,
    CONSTRAINT fk_log_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE SET NULL
);

-- Data awal
INSERT INTO gedung (kode_gedung, nama_gedung, jenis_gedung) VALUES
('GA', 'Gedung Anggrek', 'Putra'),
('GB', 'Gedung Bougenville', 'Putri');

INSERT INTO kamar (nomor_kamar, kapasitas, kode_gedung) VALUES
('101', 2, 'GA'),
('102', 2, 'GA'),
('201', 2, 'GB');

INSERT INTO users (id, nama, role, username, password, nim, prodi, no_telepon, nomor_kamar) VALUES
('U001', 'Rasendriya Oman', 'Mahasiswa', 'rasen', '12345', '103012400213', 'Informatika', '081234567890', '101'),
('U002', 'Hikmatyar Mangala', 'Mahasiswa', 'hikma', '12345', '103012400266', 'Informatika', '081298765432', '102');

INSERT INTO users (id, nama, role, username, password, nomor_sr, kode_gedung_tugas) VALUES
('SR001', 'Senior Resident GA', 'SeniorResident', 'sr_ga', '12345', 'SR-01', 'GA');

INSERT INTO users (id, nama, role, username, password, nomor_pegawai) VALUES
('PG001', 'Paul Deniel', 'PengelolaGedung', 'paul', '12345', 'PG-77');

INSERT INTO izin_keluar (izin_id, mahasiswa_id, tujuan, waktu_keluar, waktu_kembali, status) VALUES
('IZN-001', 'U001', 'Pulang Akhir Pekan', NOW(), DATE_ADD(NOW(), INTERVAL 3 HOUR), 'DIAJUKAN');

INSERT INTO facility_report (report_id, mahasiswa_id, deskripsi, lokasi, tanggal_lapor, status) VALUES
('RPT-001', 'U001', 'Lampu kamar mati', 'Kamar 101', NOW(), 'BARU');

INSERT INTO payment (payment_id, mahasiswa_id, jumlah_tagihan, denda, status) VALUES
('PAY-001', 'U001', 750000, 0, 'BELUM_BAYAR');

INSERT INTO activity_log (log_id, user_id, aktivitas, keterangan, waktu_aktivitas) VALUES
('LOG-001', 'U001', 'Login sistem', 'Akses dashboard', NOW());

-- Untuk data dummy lengkap (300+ record), import file: data_dummy_300.sql
