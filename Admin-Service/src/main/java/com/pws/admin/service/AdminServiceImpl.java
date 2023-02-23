package com.pws.admin.service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.pws.admin.dto.PermissionDTO;
import com.pws.admin.dto.SignUpDTO;
import com.pws.admin.dto.UpdatePasswordDTO;
import com.pws.admin.dto.UserBasicDetailsDTO;
import com.pws.admin.dto.UserRoleXrefDTO;
import com.pws.admin.entity.Module;
import com.pws.admin.entity.Permission;
import com.pws.admin.entity.Role;
import com.pws.admin.entity.Skill;
import com.pws.admin.entity.User;
import com.pws.admin.entity.UserRoleXref;
import com.pws.admin.exception.config.PWSException;
import com.pws.admin.repository.ModuleRepository;
import com.pws.admin.repository.PermissionRepository;
import com.pws.admin.repository.RoleRepository;
import com.pws.admin.repository.SkillRepository;
import com.pws.admin.repository.UserRepository;
import com.pws.admin.repository.UserRoleXrefRepository;
import com.pws.admin.utility.DateUtils;

/**
 * @Author Vinayak M
 * @Date 09/01/23
 */

@Service
public class AdminServiceImpl implements AdminService {



	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private ModuleRepository moduleRepository;

	@Autowired
	private UserRoleXrefRepository userRoleXrefRepository;

	@Autowired
	private PermissionRepository permissionRepository;
	
	@Autowired
	private SkillRepository skillRepository;
	Refill refill = Refill.of(5, Duration.ofMinutes(1));
	private Bucket bucket = Bucket4j.builder().addLimit(Bandwidth.classic(5, refill)).build();

	@Override
	public void UserSignUp(SignUpDTO signupDTO) throws PWSException {

		Optional<User> optionalUser = userRepository.findUserByEmail(signupDTO.getEmail());
		if (optionalUser.isPresent())
			throw new PWSException("User Already Exist with Email : " + signupDTO.getEmail());
		User user = new User();
		user.setDateOfBirth(DateUtils.getUtilDateFromString(signupDTO.getDateOfBirth()));
		user.setFirstName(signupDTO.getFirstName());
		user.setIsActive(true);
		user.setLastName(signupDTO.getLastName());
		user.setEmail(signupDTO.getEmail());
		user.setPhoneNumber(signupDTO.getPhoneNumber());
		PasswordEncoder encoder = new BCryptPasswordEncoder(8);
		// Set new password
		user.setPassword(encoder.encode(signupDTO.getPassword()));

		userRepository.save(user);
	}

	@Override
	@CachePut(cacheNames = "User",key="#email")
	public void updateUserPassword(UpdatePasswordDTO userPasswordDTO) throws PWSException {
		Optional<User> optionalUser = userRepository.findUserByEmail(userPasswordDTO.getUserEmail());

		PasswordEncoder encoder = new BCryptPasswordEncoder();
		User user = null;
		if (!optionalUser.isPresent()) {
			throw new PWSException("User Not present ");
		}
		user = optionalUser.get();
		if (encoder.matches(userPasswordDTO.getOldPassword(), user.getPassword())) {
			if (userPasswordDTO.getNewPassword().equals(userPasswordDTO.getConfirmNewPassword())) {

				user.setPassword(encoder.encode(userPasswordDTO.getConfirmNewPassword()));
				userRepository.save(user);
			} else {
				throw new PWSException("new password and confirm password doesnot match ");
			}

		} else {
			throw new PWSException("oldpassword not matched");
		}

	}

	@Override
	public void addRole(Role role) throws PWSException {
		role.setIsActive(true);
		roleRepository.save(role);
	}

	@Override
	public Optional<User> isUserRegistered(String sender) throws PWSException {
		Optional<User> optionalUser = userRepository.findUserByEmail(sender);
		if(optionalUser.isPresent())
		{
		}

		return optionalUser;
	}


	@Override
	@CachePut(cacheNames = "Role")
	public void updateRole(Role role) throws PWSException {
		roleRepository.save(role);
	}

	@Override
	@Cacheable(cacheNames = "Role")
	public ResponseEntity<List<Role>> fetchAllRole() throws PWSException {
		if(bucket.tryConsume(1)) {
			List<Role> rolelist = roleRepository.findAll();
			return new ResponseEntity<>(rolelist, HttpStatus.OK);
		} else {
			return new ResponseEntity<>(HttpStatus.TOO_MANY_REQUESTS);
		}

	}

	@Override
	@Cacheable(cacheNames = "Role", key="#id")
	public Optional<Role> fetchRoleById(Integer id) throws PWSException {
		return roleRepository.findById(id);
	}

	@Override
	@CachePut(cacheNames = "Role",key="#id")
	public void deactivateOrActivateRoleById(Integer id, boolean flag) throws PWSException {
		Optional<Role> role = roleRepository.findById(id);
		if (role.isPresent()) {
			role.get().setIsActive(flag);
			roleRepository.save(role.get());
		}
	}

	@Override
	public void addModule(Module module) throws PWSException {
		moduleRepository.save(module);
	}

	@Override
	@CachePut(cacheNames = "Module")
	public void updateRole(Module module) throws PWSException {
		Optional<Module> optionalModule = moduleRepository.findById(module.getId());
		if (optionalModule.isPresent()) {
			moduleRepository.save(module);
		} else
			throw new PWSException("Module Doest Exist");

	}

	@Override
	@Cacheable(cacheNames = "Module")
	public ResponseEntity<List<Module>> fetchAllModule() throws PWSException {
		if(bucket.tryConsume(1)) {
			List<Module> modulelist = moduleRepository.findAll();
			return new ResponseEntity<>(modulelist, HttpStatus.OK);
		} else {
			return new ResponseEntity<>(HttpStatus.TOO_MANY_REQUESTS);
		}
	}

	@Override
	@Cacheable(cacheNames = "Module",key="#id")
	public Optional<Module> fetchModuleById(Integer id) throws PWSException {
		Optional<Module> optionalModule = moduleRepository.findById(id);
		if (optionalModule.isPresent()) {
			return optionalModule;
		} else
			throw new PWSException("Module Doest Exist");
	}

	@Override
	@CachePut(cacheNames = "Module",key="#id")
	public void deactivateOrActivateModuleById(Integer id, boolean flag) throws PWSException {
		Optional<Module> optionalModule = moduleRepository.findById(id);
		Module module = optionalModule.get();
		if (optionalModule.isPresent()) {
			module.setIsActive(flag);
			moduleRepository.save(module);
		} else
			throw new PWSException("Module Doest Exist");
	}

	@Override
	public void saveOrUpdateUserXref(UserRoleXrefDTO userRoleXrefDTO) throws PWSException {
		Optional<UserRoleXref> optionalUserRoleXref = userRoleXrefRepository.findById(userRoleXrefDTO.getId());
		UserRoleXref userRoleXref = null;
		if (optionalUserRoleXref.isPresent()) {
			userRoleXref = optionalUserRoleXref.get();
		} else {
			userRoleXref = new UserRoleXref();
		}
		Optional<User> optionalUser = userRepository.findById(userRoleXrefDTO.getUserId());
		if (optionalUser.isPresent()) {
			userRoleXref.setUser(optionalUser.get());
		} else {
			throw new PWSException("User Doest Exist");
		}

		Optional<Role> optionalRole = roleRepository.findById(userRoleXrefDTO.getRoleId());
		if (optionalRole.isPresent()) {
			userRoleXref.setRole(optionalRole.get());
		} else {
			throw new PWSException("Role Doest Exist");
		}
		userRoleXref.setIsActive(userRoleXrefDTO.getIsActive());

		userRoleXrefRepository.save(userRoleXref);

	}

	@Override
	public void deactivateOrActivateAssignedRoleToUser(Integer id, Boolean flag) throws PWSException {
		Optional<UserRoleXref> optionalUserRoleXref = userRoleXrefRepository.findById(id);
		UserRoleXref userRoleXref = optionalUserRoleXref.get();
		if (optionalUserRoleXref.isPresent()) {
			optionalUserRoleXref.get().setIsActive(flag);
			userRoleXrefRepository.save(userRoleXref);
		} else
			throw new PWSException("Record Doest Exist");

	}

	@Override
	@Cacheable(cacheNames = "UserRoleXref",key="#id")
	public Optional<UserRoleXref> fetchUserById(Integer Id) throws PWSException {
		return userRoleXrefRepository.findById(Id);

	}

	@Override
	@Cacheable(cacheNames = "User",key="#roleId")
	public List<User> fetchUserByRole(Integer roleId) throws PWSException {
		return userRoleXrefRepository.fetchAllUsersByRoleId(roleId);
	}

	@Override
	public void addPermission(PermissionDTO permissionDTO) throws PWSException {
		Permission permission = new Permission();

		permission.setIsActive(permissionDTO.getIsActive());
		permission.setIsAdd(permissionDTO.getIsAdd());
		permission.setIsDelete(permissionDTO.getIsDelete());
		permission.setIsUpdate(permissionDTO.getIsUpdate());
		permission.setIsView(permissionDTO.getIsView());
		Optional<Module> module = moduleRepository.findById(permissionDTO.getModule());
		permission.setModule(module.get());
		Optional<Role> role = roleRepository.findById(permissionDTO.getRole());
		permission.setRole(role.get());
		permissionRepository.save(permission);

	}

	@Override
	@CachePut(cacheNames = "Permission")
	public void updatePermission(PermissionDTO permissionDTO) throws PWSException {
		Optional<Permission> optionalPermission = permissionRepository.findById(permissionDTO.getId());
		if (optionalPermission.isPresent()) {
			optionalPermission.get().getId();
			optionalPermission.get().setIsActive(permissionDTO.getIsActive());
			optionalPermission.get().setIsAdd(permissionDTO.getIsAdd());
			optionalPermission.get().setIsDelete(permissionDTO.getIsDelete());
			optionalPermission.get().setIsUpdate(permissionDTO.getIsUpdate());
			optionalPermission.get().setIsView(permissionDTO.getIsView());
			Optional<Module> module = moduleRepository.findById(permissionDTO.getModule());
			optionalPermission.get().setModule(module.get());
			Optional<Role> role = roleRepository.findById(permissionDTO.getRole());
			optionalPermission.get().setRole(role.get());
			permissionRepository.save(optionalPermission.get());

		} else {
			throw new PWSException("Record Doest Exist");
		}

	}

	@Override
	@Cacheable(cacheNames = "Permission")
	public	ResponseEntity<List<Permission>> fetchAllPermission() throws PWSException {
		if(bucket.tryConsume(1)) {
			List<Permission> permissionlist = permissionRepository.findAll();
			return new ResponseEntity<>(permissionlist, HttpStatus.OK);
		} else {
			return new ResponseEntity<>(HttpStatus.TOO_MANY_REQUESTS);
		}


	}

	@Override
	@Cacheable(cacheNames = "User",key="#id")
	public Optional<Permission> fetchPermissionById(Integer id) throws PWSException {
		Optional<Permission> optionalPermission = permissionRepository.findById(id);
		if (optionalPermission.isPresent()) {
			return optionalPermission;
		} else
			throw new PWSException("Permission Does't Exist");
	}

	@Override
	@CachePut(cacheNames = "Permission",key="#id")
	public void deactivateOrActivatePermissionById(PermissionDTO permissionDTO) throws PWSException {
		Optional<Permission> optionalPermission = permissionRepository.findById(permissionDTO.getId());
		Permission permission = null;
		if (optionalPermission.isPresent()) {
			permission = optionalPermission.get();
			permission.setIsActive(permissionDTO.getIsActive());
			permissionRepository.save(permission);
		} else

			throw new PWSException("Permission Id Doest Exist");

	}

	@Override
	public UserBasicDetailsDTO getUserBasicInfoAfterLoginSuccess(String email) throws PWSException {
	    Optional<User> optionalUser = userRepository.findUserByEmail(email);
	    if(! optionalUser.isPresent())
	        throw new PWSException("User Already Exist with Email : " + email);


	    User user = optionalUser.get();
	    UserBasicDetailsDTO userBasicDetailsDTO =new UserBasicDetailsDTO();
	    userBasicDetailsDTO.setUser(user);

	    List<Role> roleList = userRoleXrefRepository.findAllUserRoleByUserId(user.getId());
	    userBasicDetailsDTO.setRoleList(roleList);
	    List<Permission> permissionList =null;
	    if(roleList.size()>0)
	     permissionList = permissionRepository.getAllUserPermisonsByRoleId(roleList.get(0).getId());

	    userBasicDetailsDTO.setPermissionList(permissionList);
	    return userBasicDetailsDTO;
	}

	@Override
	public void addSkill(Skill skill) throws PWSException {
		skill.setIsActive(true);
		skillRepository.save(skill);		
	}

	@Override
	@CachePut(cacheNames = "Skill")
	public void updateSkill(Skill skill) throws PWSException {
		Optional<Skill> optionalSkill= skillRepository.findById(skill.getId());
		if(optionalSkill.isPresent()) {
		skillRepository.save(skill);
		}else
			throw new PWSException("Skill doesn't exist");
	}

	@Override
	@Cacheable(cacheNames = "Skill")
	public ResponseEntity<List<Skill>> fetchAllSkills() throws PWSException {
		if(bucket.tryConsume(1)) {
			List<Skill> skilllist = skillRepository.findAll();
			return new ResponseEntity<>(skilllist, HttpStatus.OK);
		} else {
			return new ResponseEntity<>(HttpStatus.TOO_MANY_REQUESTS);
		}
	}

	@Override
//	@Cacheable(cacheNames = "Skill",key="#id")
	public Optional<Skill> fetchskillById(Integer id) throws PWSException {
		Optional<Skill> optionalskill= skillRepository.findById(id);
		if(optionalskill.isPresent()) {
		return skillRepository.findById(id);
		}else
			throw new PWSException("Skill doesn't exist");
	}

	@Override
	@CacheEvict(cacheNames = "Skill", key = "#id")
	public void deleteskillById(Integer id) throws PWSException {
		skillRepository.deleteById(id);
	}


}
