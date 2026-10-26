package com.futuremedia.services;

import com.futuremedia.models.BadRequestException;
import com.futuremedia.models.Brand;
import com.futuremedia.models.ContactUs;
import com.futuremedia.models.ResourceNotFoundException;
import com.futuremedia.models.SalesOption;
import com.futuremedia.models.ServiceOption;
import com.futuremedia.repositories.BrandRepository;
import com.futuremedia.repositories.ContactUsRepository;
import com.futuremedia.repositories.SalesOptionRepository;
import com.futuremedia.repositories.ServiceOptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class ContactUsService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Autowired
    private ContactUsRepository contactUsRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private SalesOptionRepository salesOptionRepository;

    @Autowired
    private ServiceOptionRepository serviceOptionRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private LeadService leadService;

    @Transactional
    public ContactUs submit(String name, String email, String telephone, String company,
                            Long brandId, Long salesId, Long serviceId) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Name is required");
        }
        if (email == null || email.isBlank() || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new BadRequestException("Valid email is required");
        }
        if (telephone == null || telephone.isBlank()) {
            throw new BadRequestException("Telephone is required");
        }
        if (company == null || company.isBlank()) {
            throw new BadRequestException("Company you represent is required");
        }
        if (brandId == null) {
            throw new BadRequestException("Brand is required");
        }
        if (salesId == null) {
            throw new BadRequestException("Sales option is required");
        }
        if (serviceId == null) {
            throw new BadRequestException("Service is required");
        }

        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + brandId));
        SalesOption sales = salesOptionRepository.findById(salesId)
                .orElseThrow(() -> new ResourceNotFoundException("Sales option not found with id: " + salesId));
        ServiceOption service = serviceOptionRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + serviceId));

        if (!brand.isActive()) {
            throw new BadRequestException("Selected brand is not available");
        }
        if (!sales.isActive()) {
            throw new BadRequestException("Selected sales option is not available");
        }
        if (!service.isActive()) {
            throw new BadRequestException("Selected service is not available");
        }

        ContactUs contact = new ContactUs();
        contact.setName(name.trim());
        contact.setEmail(email.trim().toLowerCase());
        contact.setTelephone(telephone.trim());
        contact.setCompany(company.trim());
        contact.setBrand(brand);
        contact.setSales(sales);
        contact.setService(service);
        contact.setEmailSent(false);

        return contactUsRepository.save(contact);
    }

    public ContactUs submitAndNotify(String name, String email, String telephone, String company,
                                     Long brandId, Long salesId, Long serviceId) {
        ContactUs saved = submit(name, email, telephone, company, brandId, salesId, serviceId);
        leadService.captureQuietly(saved.getName(), saved.getEmail(), saved.getTelephone(),
                saved.getCompany(), "Contact Us");
        emailService.sendContactUsEmail(saved);
        return saved;
    }

    @Transactional
    public void markEmailSent(Long id) {
        contactUsRepository.findById(id).ifPresent(c -> {
            c.setEmailSent(true);
            contactUsRepository.save(c);
        });
    }

    public List<ContactUs> getAll() {
        return contactUsRepository.findAllByOrderBySubmittedDateDesc();
    }

    public ContactUs getById(Long id) {
        return contactUsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact submission not found with id: " + id));
    }

    public void delete(Long id) {
        ContactUs contact = getById(id);
        contactUsRepository.delete(contact);
    }
}
