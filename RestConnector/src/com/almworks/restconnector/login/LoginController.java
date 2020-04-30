package com.almworks.restconnector.login;

import com.almworks.restconnector.JiraCredentials;
import com.almworks.util.threads.CanBlock;
import org.jetbrains.annotations.NotNull;

public interface LoginController {
  /**
   * @param login user login name
   * @return not null message means that login should not be performed due to password is invalid<br>
   *   null means that login should proceed
   */
  String getInvalidLoginMessage(String login);

  /**
   * Notification that login attempt just failed.<br>
   * The method may block current thread to ask user to check and update credentials. If user has updated the credentials the method returns it.
   * @param credentials wrong credential which caused authentication problem
   * @param message server reply or other user-displayable explanation what has gone wrong
   * @return null if login attempt should be rejected.<br>
   *   Not null result is new credentials, login attempt should be retried. Pair with both elements empty or null means anonymous connection
   */
  @CanBlock
  JiraCredentials loginInvalid(@NotNull LoginJiraCredentials credentials, String message) throws InterruptedException;

  /**
   * Simple implementation. Stores invalid message. Should be used when no global login state is stored.
   */
  class Dummy implements LoginController {
    private String myInvalidMessage = null;

    @Override
    public String getInvalidLoginMessage(String login) {
      return myInvalidMessage;
    }

    @Override
    public JiraCredentials loginInvalid(@NotNull LoginJiraCredentials credentials, String message) {
      myInvalidMessage = message;
      return null;
    }
  }
}
