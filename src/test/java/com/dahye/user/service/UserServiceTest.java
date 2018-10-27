package com.dahye.user.service;

import com.dahye.main.MailSender;
import com.dahye.user.dao.UserDao;
import com.dahye.user.domain.Grade;
import com.dahye.user.domain.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.dahye.user.service.UserService.MIN_LOGCOUNT_FOR_SILVER;
import static com.dahye.user.service.UserService.MIN_RECOMMEND_FOR_GOLD;
import static junit.framework.Assert.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "file:src/main/resources/applicationContext-*.xml")
public class UserServiceTest {

    @Autowired
    UserService userService;

    @Autowired
    UserDao userDao;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    MailSender mailSender;

    List<User> users;

    static class TestUserService extends UserService {
        private String id;

        private TestUserService(String id) {
            this.id = id;
        }

        protected void upgradeGrade(User user) {
            if(user.getId().equals(this.id)) throw new TestUserServiceException();
            super.upgradeGrade(user);
        }
    }

    static class TestUserServiceException extends RuntimeException {

    }

    static class MockMailSender implements MailSender {
        private List<String> req = new ArrayList<String>();

        public List<String> getReq() {
            return req;
        }

        public void send(SimpleMailMessage simpleMessage) throws MailException {
            req.add(simpleMessage.getTo()[0]);
        }

        public void send(SimpleMailMessage[] simpleMessages) throws MailException {

        }
    }

    @Before
    public void setUp() {
        users = Arrays.asList(
                new User("dahye1", "김다혜1", "p1", Grade.BASIC, MIN_LOGCOUNT_FOR_SILVER-1, 0, "dahye1@dahye.com"),
                new User("dahye2", "김다혜2", "p2", Grade.BASIC, MIN_LOGCOUNT_FOR_SILVER, 0, "dahye2@dahye.com"),
                new User("dahye3", "김다혜3", "p3", Grade.SILVER, 60, MIN_RECOMMEND_FOR_GOLD-1, "dahye3@dahye.com"),
                new User("dahye4", "김다혜4", "p4", Grade.SILVER, 60, MIN_RECOMMEND_FOR_GOLD, "dahye4@dahye.com"),
                new User("dahye5", "김다혜5", "p5", Grade.GOLD, 100, Integer.MAX_VALUE, "dahye5@dahye.com")
        );
    }

    @Test
    @DirtiesContext
    public void upgradeGrade() throws Exception {
        userDao.deleteAll();

        for(User user : users) userDao.add(user);

        MockMailSender mockMailSender = new MockMailSender();
        userService.setMailSender(mockMailSender);

        userService.upgradeGrades();

        checkGradeUpgrade(users.get(0), false);
        checkGradeUpgrade(users.get(1), true);
        checkGradeUpgrade(users.get(2), false);
        checkGradeUpgrade(users.get(3), true);
        checkGradeUpgrade(users.get(4), false);

        List<String> request = mockMailSender.getReq();
        assertThat(request.size(), is(2));
        assertThat(request.get(0), is(users.get(1).getEmail()));
        assertThat(request.get(1), is(users.get(3).getEmail()));
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

    @Test
    public void upgradeAllOrNothing() throws Exception {
        UserService testUserService = new TestUserService(users.get(3).getId());
        testUserService.setUserDao(this.userDao);
        testUserService.setTransactionManager(this.transactionManager);
        testUserService.setMailSender(this.mailSender);
        userDao.deleteAll();
        for(User user : users) userDao.add(user);

        try {
            testUserService.upgradeGrades();
            fail("TestUserServiceException expected");
        } catch (TestUserServiceException e) {

        }

        checkGradeUpgrade(users.get(1), false);
    }
}
