package com.loanpro.config;

import com.loanpro.modules.catalog.domain.LoanProduct;
import com.loanpro.modules.catalog.repository.LoanProductRepository;
import com.loanpro.modules.customer.domain.CustomerProfile;
import com.loanpro.modules.customer.domain.EmploymentType;
import com.loanpro.modules.customer.repository.CustomerProfileRepository;
import com.loanpro.modules.identity.domain.Role;
import com.loanpro.modules.identity.domain.RoleName;
import com.loanpro.modules.identity.domain.User;
import com.loanpro.modules.identity.domain.UserStatus;
import com.loanpro.modules.identity.repository.RoleRepository;
import com.loanpro.modules.identity.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AppProperties properties;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final LoanProductRepository loanProductRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(
            AppProperties properties,
            RoleRepository roleRepository,
            UserRepository userRepository,
            CustomerProfileRepository customerProfileRepository,
            LoanProductRepository loanProductRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.properties = properties;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.loanProductRepository = loanProductRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.seed().enabled()) {
            return;
        }
        seedRoles();
        seedUsers();
        seedProducts();
    }

    private void seedRoles() {
        createRole(RoleName.CUSTOMER, "Loan applicant");
        createRole(RoleName.MAKER, "First-level application reviewer");
        createRole(RoleName.CHECKER, "Second-level approver");
        createRole(RoleName.ADMIN, "System administrator");
    }

    private void createRole(RoleName name, String description) {
        if (!roleRepository.existsByName(name)) {
            Role role = new Role();
            role.setName(name);
            role.setDescription(description);
            roleRepository.save(role);
        }
    }

    private void seedUsers() {
        createUser("admin@loanpro.com", "Amina", "Okoye", "Admin@12345", Set.of(RoleName.ADMIN), false, null);
        createUser("maker@loanpro.com", "Marcus", "Adeyemi", "Maker@12345", Set.of(RoleName.MAKER), false, null);
        createUser("checker@loanpro.com", "Priya", "Sharma", "Checker@12345", Set.of(RoleName.CHECKER), false, null);
        createUser("customer@loanpro.com", "Ajeet", "Gupta", "Customer@12345", Set.of(RoleName.CUSTOMER), true, this::populateDemoCustomer);
        renameIfPresent("customer@loanpro.com", null, "Ajeet", "Gupta");
        renameIfPresent("maker@loanpro.com", null, "Marcus", "Adeyemi");
        renameIfPresent("checker@loanpro.com", null, "Priya", "Sharma");
        renameIfPresent("admin@loanpro.com", null, "Amina", "Okoye");
    }

    private void renameIfPresent(String email, String onlyIfFirstName, String firstName, String lastName) {
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            if (onlyIfFirstName == null || onlyIfFirstName.equalsIgnoreCase(user.getFirstName())) {
                user.setFirstName(firstName);
                user.setLastName(lastName);
            }
        });
    }

    private void populateDemoCustomer(CustomerProfile profile) {
        profile.setDateOfBirth(LocalDate.of(1994, 4, 12));
        profile.setGender("Male");
        profile.setNationalId("A1234567");
        profile.setAddressLine("18 Marina Boulevard");
        profile.setCity("Lagos");
        profile.setState("Lagos");
        profile.setPostalCode("100001");
        profile.setEmploymentType(EmploymentType.SALARIED);
        profile.setEmployerName("Northwind Bank");
        profile.setDesignation("Relationship Manager");
        profile.setYearsEmployed(5);
        profile.setMonthlyIncome(new BigDecimal("185000"));
        profile.setOtherIncome(new BigDecimal("15000"));
        profile.setExistingEmis(new BigDecimal("22000"));
        profile.setMonthlyExpenses(new BigDecimal("70000"));
    }

    private void createUser(
            String email,
            String first,
            String last,
            String password,
            Set<RoleName> roles,
            boolean customer,
            java.util.function.Consumer<CustomerProfile> profileCustomizer
    ) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        User user = new User();
        user.setEmail(email);
        user.setFirstName(first);
        user.setLastName(last);
        user.setPhone("+234800000000");
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(roles.iterator().next());
        roles.forEach(name -> user.getRoles().add(roleRepository.findByName(name).orElseThrow()));
        userRepository.save(user);
        if (customer) {
            CustomerProfile profile = new CustomerProfile();
            profile.setUser(user);
            if (profileCustomizer != null) {
                profileCustomizer.accept(profile);
            }
            customerProfileRepository.save(profile);
        }
        log.info("Seeded user {}", email);
    }

    private void seedProducts() {
        createProduct("PERSONAL", "Personal Loan", "Unsecured personal loan for salaried customers",
                "50000", "2000000", 6, 60, "14.500", "3.000", "IDENTITY,ADDRESS_PROOF,INCOME_PROOF");
        createProduct("HOME", "Home Loan", "Long-tenure home financing with competitive rates",
                "500000", "15000000", 60, 360, "9.750", "1.000", "IDENTITY,ADDRESS_PROOF,INCOME_PROOF,BANK_STATEMENT");
        createProduct("AUTO", "Auto Loan", "Vehicle financing with flexible EMI options",
                "200000", "8000000", 12, 84, "11.250", "1.500", "IDENTITY,INCOME_PROOF,BANK_STATEMENT");
    }

    private void createProduct(
            String code, String name, String description,
            String min, String max, int minT, int maxT, String rate, String fee, String docs
    ) {
        if (loanProductRepository.existsByCodeIgnoreCase(code)) {
            return;
        }
        LoanProduct product = new LoanProduct();
        product.setCode(code);
        product.setName(name);
        product.setDescription(description);
        product.setMinAmount(new BigDecimal(min));
        product.setMaxAmount(new BigDecimal(max));
        product.setMinTenureMonths(minT);
        product.setMaxTenureMonths(maxT);
        product.setInterestRate(new BigDecimal(rate));
        product.setProcessingFeePercent(new BigDecimal(fee));
        product.setRequiredDocuments(docs);
        product.setActive(true);
        loanProductRepository.save(product);
    }
}
