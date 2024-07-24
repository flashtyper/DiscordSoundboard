package net.dirtydeeds.discordsoundboard.service.impl;

import net.dirtydeeds.discordsoundboard.beans.MyUser;
import net.dirtydeeds.discordsoundboard.repository.UserRepository;
import net.dirtydeeds.discordsoundboard.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@SuppressWarnings("unused")
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public Optional<MyUser> findById(String id) {
        return userRepository.findById(id);
    }

    @Override
    public Iterable<MyUser> saveAll(List<MyUser> myUsers) {
        return userRepository.saveAll(myUsers);
    }

    @Override
    public MyUser findOneByIdOrUsernameIgnoreCase(String userNameOrId, String userNameOrId1) {
        return userRepository.findOneByIdOrUsernameIgnoreCase(userNameOrId, userNameOrId1);
    }

    @Override
    public MyUser save(MyUser myUser) {
        return userRepository.save(myUser);
    }

    @Override
    public Iterable<MyUser> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
}
