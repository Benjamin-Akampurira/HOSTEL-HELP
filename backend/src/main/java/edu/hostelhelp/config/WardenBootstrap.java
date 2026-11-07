package edu.hostelhelp.config;

import edu.hostelhelp.entity.Warden;
import edu.hostelhelp.repository.UserRepository;
import edu.hostelhelp.repository.WardenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class WardenBootstrap implements CommandLineRunner {
    private final WardenRepository wardens;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String fullName;
    private final String staffNumber;
    private final String phoneNumber;

    public WardenBootstrap(WardenRepository wardens, UserRepository users, PasswordEncoder passwordEncoder,
                           @Value("${HOSTEL_WARDEN_EMAIL:}") String email,
                           @Value("${HOSTEL_WARDEN_PASSWORD:}") String password,
                           @Value("${HOSTEL_WARDEN_NAME:}") String fullName,
                           @Value("${HOSTEL_WARDEN_STAFF_NUMBER:}") String staffNumber,
                           @Value("${HOSTEL_WARDEN_PHONE:}") String phoneNumber) {
        this.wardens = wardens;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.email = email.trim().toLowerCase();
        this.password = password;
        this.fullName = fullName.trim();
        this.staffNumber = staffNumber.trim();
        this.phoneNumber = phoneNumber.trim();
    }

    @Override
    public void run(String... args) {
        if (email.isBlank()) return;
        var existing = users.findByEmail(email);
        if (existing.isPresent()) {
            if (!(existing.get() instanceof Warden warden)) {
                throw new IllegalStateException("The configured warden email belongs to a non-warden account.");
            }
            if (!fullName.isBlank()) warden.setFullName(fullName);
            if (!staffNumber.isBlank()) warden.setStaffNumber(staffNumber);
            if (!phoneNumber.isBlank()) warden.setPhoneNumber(phoneNumber);
            wardens.save(warden);
            return;
        }
        if (password.isBlank()) return;
        if (password.length() < 12 || fullName.isBlank()) {
            throw new IllegalStateException("Configure warden name and a fresh password of at least 12 characters to create the account.");
        }
        Warden warden = new Warden();
        warden.setEmail(email);
        warden.setFullName(fullName);
        warden.setPasswordHash(passwordEncoder.encode(password));
        if (!staffNumber.isBlank()) warden.setStaffNumber(staffNumber);
        if (!phoneNumber.isBlank()) warden.setPhoneNumber(phoneNumber);
        wardens.save(warden);
    }
}