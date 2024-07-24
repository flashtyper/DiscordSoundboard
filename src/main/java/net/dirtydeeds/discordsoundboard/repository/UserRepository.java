package net.dirtydeeds.discordsoundboard.repository;

import net.dirtydeeds.discordsoundboard.beans.MyUser;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface UserRepository extends PagingAndSortingRepository<MyUser, String>, CrudRepository<MyUser,String> {
    MyUser findOneByIdOrUsernameIgnoreCase(String id, String userName);
}
