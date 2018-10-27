package com.dahye.user.service;

import com.dahye.user.dao.UserDao;
import com.dahye.user.domain.Grade;
import com.dahye.user.domain.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import static com.dahye.user.service.UserService.MIN_LOGCOUNT_FOR_SILVER;
import static com.dahye.user.service.UserService.MIN_RECOMMEND_FOR_GOLD;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "file:src/main/resources/applicationContext-*.xml")
public class UserServiceTest {

    @Autowired
    UserService userService;

    @Autowired
    UserDao userDao;

    List<User> users;

    @Before
    public void setUp() {
        users = Arrays.asList(
                new User("dahye1", "김다혜1", "p1", Grade.BASIC, MIN_LOGCOUNT_FOR_SILVER-1, 0),
                new User("dahye2", "김다혜2", "p2", Grade.BASIC, MIN_LOGCOUNT_FOR_SILVER, 0),
                new User("dahye3", "김다혜3", "p3", Grade.SILVER, 60, MIN_RECOMMEND_FOR_GOLD-1),
                new User("dahye4", "김다혜4", "p4", Grade.SILVER, 60, MIN_RECOMMEND_FOR_GOLD),
                new User("dahye5", "김다혜5", "p5", Grade.GOLD, 100, Integer.MAX_VALUE)
        );
    }

    @Test
    public void upgradeGrade() {
        userDao.deleteAll();

        for(User user : users) userDao.add(user);

        userService.upgradeGrades();

        checkGradeUpgrade(users.get(0), false);
        checkGradeUpgrade(users.get(1), true);
        checkGradeUpgrade(users.get(2), false);
        checkGradeUpgrade(users.get(3), true);
        checkGradeUpgrade(users.get(4), false);
    }

    private void checkGradeUpgrade(User user, boolean changed) {
        User userUpdate = userDao.get(user.getId());
        if (changed) {
            assertThat(userUpdate.getGrade(), is(user.getGrade().nextGrade()));
        } else {
            assertThat(userUpdate.getGrade(), is(user.getGrade()));
        }
    }

    @Test
    public void add() {
        userDao.deleteAll();

        User userWithGrade = users.get(4);
        User userWithoutGrade = users.get(0);
        userWithoutGrade.setGrade(null);

        userService.add(userWithGrade);
        userService.add(userWithoutGrade);

        User userWithGradeRead = userDao.get(userWithGrade.getId());
        User userWithoutGradeRead = userDao.get(userWithoutGrade.getId());

        assertThat(userWithGradeRead.getGrade(), is(userWithGrade.getGrade()));
        assertThat(userWithoutGradeRead.getGrade(), is(Grade.BASIC));
    }
}
