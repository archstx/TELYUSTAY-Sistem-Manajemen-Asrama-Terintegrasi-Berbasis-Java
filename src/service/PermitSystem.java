package asrama.interfacepkg;

import asrama.model.Mahasiswa;

public interface PermitSystem {
    boolean applyPermit(Mahasiswa mahasiswa);

    boolean validateStatus(Mahasiswa mahasiswa);
}
