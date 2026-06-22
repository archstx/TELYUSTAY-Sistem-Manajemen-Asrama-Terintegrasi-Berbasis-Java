package asrama.model;

public abstract class User {
    private final String id;
    private String nama;
    private final String role;
    private String username;
    private String password;
    private boolean loggedIn;

    protected User(String id, String nama, String role) {
        this.id = id;
        this.nama = nama;
        this.role = role;
    }

    public String getId() {
        return id;
    }

    public String getNama() {
        return nama;
    }

    public String getRole() {
        return role;
    }

    public void setNama(String nama) {
        this.nama = nama;
    }

    public void setCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public boolean login(String username, String password) {
        if (this.username == null || this.password == null) {
            return false;
        }
        this.loggedIn = this.username.equals(username) && this.password.equals(password);
        return loggedIn;
    }

    public void logout() {
        this.loggedIn = false;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public abstract void tampilkanDashboard();
}
