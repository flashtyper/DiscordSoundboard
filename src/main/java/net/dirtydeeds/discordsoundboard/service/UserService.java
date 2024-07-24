package net.dirtydeeds.discordsoundboard.service;

import net.dirtydeeds.discordsoundboard.beans.MyUser;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserService {

    Optional<MyUser> findById(String id);

    Iterable<MyUser> saveAll(List<MyUser> myUsers);

    MyUser findOneByIdOrUsernameIgnoreCase(String userNameOrId, String userNameOrId1);

    MyUser save(MyUser myUser);

    Iterable<MyUser> findAll(Pageable pageable);
}
