package com.marklogic.envision.auth;

import com.marklogic.grove.boot.AbstractController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/auth")
public class UserController extends AbstractController {

	private final UserService userService;

	@Autowired
	UserController(UserService userService) {
		this.userService = userService;
	}

	@RequestMapping(value = "/signup", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> signup(@RequestBody UserPojo user) throws IOException {
		userService.createUser(user);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequestMapping(value = "/delete", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<?> signup(@RequestParam String username) throws IOException {
		userService.deleteUser(username);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequestMapping(value = "/userExists", method = RequestMethod.GET)
	@ResponseBody
	public boolean userExists(@RequestParam String email) {
		return userService.userExists(email);
	}

	@RequestMapping(value = "/resetPassword", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<?> resetPassword(@RequestParam String email) {
		userService.addResetTokenToUser(email);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequestMapping(value = "/validateResetToken", method = RequestMethod.GET)
	@ResponseBody
	public ValidateTokenPojo validateResetToken(@RequestParam String token) {
		return userService.validateResetToken(token);
	}

	@RequestMapping(value = "/updatePassword", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> updatePassword(@RequestBody UpdatePasswordPojo resetPwd) {
		userService.updatePassword(resetPwd.token, resetPwd.password);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequestMapping(value = "/profile", method = RequestMethod.GET)
	public UserProfile profile() {
		return userService.getUser(getHubClient().getFinalClient(), getCurrentUser()).toUserProfile();
	}
}