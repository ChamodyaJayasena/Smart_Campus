package com.smartcampus.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.smartcampus.model.UserAccount;
import com.smartcampus.model.UserRole;

public interface UserAccountRepository extends MongoRepository<UserAccount, String> {
    Optional<UserAccount> findByEmail(String email);
    List<UserAccount> findByRole(UserRole role);
}
