package com.example.Ships.Repo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.Ships.User;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
//deprecated, this is more for a learning process about autowired and dependency injection
@Deprecated
@Repository
public class SimpleRepo implements TutorialRepo{

    private JdbcTemplate template;
    @Autowired
    public SimpleRepo (DataSource datasource) {
        this.template = new JdbcTemplate(datasource);
    }
    @Override
    public int findById(long ID) {
        return 0;
    }

    @Override
    public Boolean saveUser(User user) {//use a prepared statement
        String query = "insert into ";
        return true;
    }

    @Override
    public User findByUserName(String username) {
        return null;
    }

    public boolean doesUserNameExist(String user) {
        String x = template.queryForObject("Select UserName From login where UserName = 'buckwild'",String.class);
        System.out.println(x);


        return true;
    }
}
