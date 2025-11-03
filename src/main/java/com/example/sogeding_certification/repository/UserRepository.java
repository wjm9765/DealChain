package com.example.sogeding_certification.repository;

import com.example.sogeding_certification.entity.User;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {
    private final List<User> users = new ArrayList<>();

    public void save(User user) {
        users.add(user);
    }

    public Optional<User> findByNameAndPhoneNumberAndResidentNumber(
            String name, String phoneNumber, String residentNumber) {
        return users.stream()
                .filter(user -> user.getName().equals(name) &&
                        user.getPhoneNumber().equals(phoneNumber) &&
                        user.getResidentNumber().equals(residentNumber))
                .findFirst();
    }

    public List<User> findAll() {
        return new ArrayList<>(users);
    }
}



