package com.futuremedia.config;

import com.futuremedia.models.Brand;
import com.futuremedia.models.EmailTemplate;
import com.futuremedia.models.LeadSource;
import com.futuremedia.models.Role;
import com.futuremedia.models.SalesOption;
import com.futuremedia.models.ServiceOption;
import com.futuremedia.models.ShopbeatFeeling;
import com.futuremedia.models.User;
import com.futuremedia.repositories.BrandRepository;
import com.futuremedia.repositories.EmailTemplateRepository;
import com.futuremedia.repositories.LeadSourceRepository;
import com.futuremedia.repositories.SalesOptionRepository;
import com.futuremedia.repositories.ServiceOptionRepository;
import com.futuremedia.repositories.ShopbeatFeelingRepository;
import com.futuremedia.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceOptionRepository serviceOptionRepository;

    @Autowired
    private ShopbeatFeelingRepository shopbeatFeelingRepository;

    @Autowired
    private LeadSourceRepository leadSourceRepository;

    @Autowired
    private EmailTemplateRepository emailTemplateRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private SalesOptionRepository salesOptionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.super-admin.email}")
    private String superAdminEmail;

    @Value("${app.super-admin.password}")
    private String superAdminPassword;

    @Value("${app.super-admin.full-name}")
    private String superAdminFullName;

    @Override
    public void run(String... args) {
        seedSuperAdmin();
        seedServices();
        seedBrands();
        seedSales();
        seedShopbeatFeelings();
        seedLeadSources();
        seedDefaultEmailTemplate();
    }

    private void seedSuperAdmin() {
        if (!userRepository.existsByEmail(superAdminEmail)) {
            User superAdmin = new User();
            superAdmin.setFullName(superAdminFullName);
            superAdmin.setEmail(superAdminEmail);
            superAdmin.setPassword(passwordEncoder.encode(superAdminPassword));
            superAdmin.setRole(Role.SUPER_ADMIN);
            superAdmin.setEnabled(true);
            userRepository.save(superAdmin);
            System.out.println("Default Super Admin created: " + superAdminEmail);
        } else {
            System.out.println("Super Admin already exists: " + superAdminEmail);
        }
    }

    private void seedServices() {
        String[] services = {
                "General",
                "Conferencing",
                "Video or Audio Production",
                "Offers"
        };
        for (String name : services) {
            if (!serviceOptionRepository.existsByNameIgnoreCase(name)) {
                ServiceOption option = new ServiceOption();
                option.setName(name);
                option.setActive(true);
                serviceOptionRepository.save(option);
                System.out.println("Seeded service: " + name);
            }
        }
    }

    private void seedBrands() {
        String[] brands = {
                "99 FM",
                "Radio Omulunga",
                "One Africa",
                "On One Sports",
                "Shop Beat",
                "Future Media"
        };
        for (String name : brands) {
            if (!brandRepository.existsByNameIgnoreCase(name)) {
                Brand brand = new Brand();
                brand.setName(name);
                brand.setActive(true);
                brandRepository.save(brand);
                System.out.println("Seeded brand: " + name);
            }
        }
    }

    private void seedSales() {
        String[] sales = {
                "Radio",
                "Television",
                "Digital",
                "Outdoor",
                "Retail Media",
                "General"
        };
        for (String name : sales) {
            if (!salesOptionRepository.existsByNameIgnoreCase(name)) {
                SalesOption option = new SalesOption();
                option.setName(name);
                option.setActive(true);
                salesOptionRepository.save(option);
                System.out.println("Seeded sales option: " + name);
            }
        }
    }

    private void seedShopbeatFeelings() {
        String[] feelings = {
                "Energetic",
                "Relaxed",
                "Sophisticated",
                "Welcoming",
                "Exciting",
                "Professional",
                "Fun",
                "Inspiring",
                "Other"
        };
        for (String name : feelings) {
            if (!shopbeatFeelingRepository.existsByNameIgnoreCase(name)) {
                ShopbeatFeeling feeling = new ShopbeatFeeling();
                feeling.setName(name);
                feeling.setActive(true);
                shopbeatFeelingRepository.save(feeling);
                System.out.println("Seeded Shopbeat feeling: " + name);
            }
        }
    }

    private void seedLeadSources() {
        String[] sources = {
                "Social Media",
                "Media Audit",
                "Gate Document Download",
                "Shopbeat",
                "Google",
                "Contact Us"
        };
        for (String name : sources) {
            if (!leadSourceRepository.existsByNameIgnoreCase(name)) {
                LeadSource source = new LeadSource();
                source.setName(name);
                source.setActive(true);
                leadSourceRepository.save(source);
                System.out.println("Seeded lead source: " + name);
            }
        }
    }

    private void seedDefaultEmailTemplate() {
        String templateName = "Future Media Broadcast";
        if (emailTemplateRepository.existsByNameIgnoreCase(templateName)) {
            return;
        }
        User admin = userRepository.findByEmail(superAdminEmail).orElse(null);
        if (admin == null) {
            System.err.println("Could not seed email template: super admin not found");
            return;
        }
        EmailTemplate template = new EmailTemplate();
        template.setName(templateName);
        template.setPreheader("The latest from Future Media — made for you, {{name}}");
        template.setHeaderHeadline("Hello {{name}}");
        template.setHeaderSubheadline("News, offers and stories from Future Media");
        template.setHeaderColor("#0b1f3a");
        template.setHeaderTextColor("#ffffff");
        template.setBodyHeadline("We have something special for you");
        template.setBodyContent(
                "Thank you for staying connected with Future Media.\n\n"
                        + "This month we are sharing fresh campaigns, production highlights and opportunities crafted for brands like {{company}}.\n\n"
                        + "If you would like to talk about your next project, we would love to hear from you.");
        template.setBodyColor("#ffffff");
        template.setBodyTextColor("#334155");
        template.setCtaText("Visit Future Media");
        template.setCtaUrl("https://futuremedia.simplyfound.com.na");
        template.setCtaButtonColor("#1a73e8");
        template.setCtaTextColor("#ffffff");
        template.setFooterText("Future Media · Namibia\nYou received this because you connected with our team.");
        template.setFooterColor("#0b1f3a");
        template.setFooterTextColor("#a8c0d8");
        template.setUseCompanyLogo(true);
        template.setActive(true);
        template.setCreatedBy(admin);
        emailTemplateRepository.save(template);
        System.out.println("Seeded default email template: " + templateName);
    }
}
