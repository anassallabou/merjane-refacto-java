package com.nimbleways.springboilerplate.services.implementations;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.nimbleways.springboilerplate.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.ProductRepository;

@Service
public class ProductService {

    @Autowired
    ProductRepository productRepository;

    @Autowired
    NotificationService notificationService;

    public void notifyDelay(int leadTime, Product p) {
        p.setLeadTime(leadTime);
        productRepository.save(p);
        notificationService.sendDelayNotification(leadTime, p.getName());
    }

    public void handleSeasonalProduct(Product p) {

        leadTimeAfterSeasonEndDate(p);

        seasonEndDateAfterTody(p);

        notifyDelay(p);
    }

    public void handleExpiredProduct(Product p) {

        expireDateIsAfterToday(p);

        sendingExprationNotification(p);

    }

    public void handlProductType(Set<Product> products){
        for (Product p : products) {
            switch (p.getType()){
                case Constants.PRODUCT_TYPE_NORMAL -> handleIfProductTypeNormal(p);
                case (Constants.PRODUCT_TYPE_SEASONAL) ->  handleIfProductTypeSeasonal(p);
                case (Constants.PRODUCT_TYPE_EXPIRABLE) ->  handleIfProductTypeExpirable(p);
                default -> throw new IllegalArgumentException("Invalid type of Product: " + p.getType());
            }
        }
    }

    private void handleIfProductTypeExpirable(Product p) {
        if (p.getAvailable() > 0 && p.getExpiryDate().isAfter(LocalDate.now())) {
            p.setAvailable(p.getAvailable() - Constants.oneToAbstruct);
            productRepository.save(p);
        } else {
            handleExpiredProduct(p);
        }
    }

    private void handleIfProductTypeSeasonal(Product p) {
        if ((LocalDate.now().isAfter(p.getSeasonStartDate()) && LocalDate.now().isBefore(p.getSeasonEndDate())
                && p.getAvailable() > Constants.zero)) {
            p.setAvailable(p.getAvailable() - Constants.oneToAbstruct);
            productRepository.save(p);
        } else {
            handleSeasonalProduct(p);
        }
    }

    private void handleIfProductTypeNormal(Product p) {
        if (p.getAvailable() > Constants.zero) {
            p.setAvailable(p.getAvailable() - Constants.oneToAbstruct);
            productRepository.save(p);
        } else {
            int leadTime = p.getLeadTime();
            if (leadTime > Constants.zero) {
                notifyDelay(leadTime, p);
            }
        }
    }

    private void seasonEndDateAfterTody(Product p) {
        if (p.getSeasonStartDate().isAfter(LocalDate.now())) {
            notificationService.sendOutOfStockNotification(p.getName());
            productRepository.save(p);
        }
    }

    private void leadTimeAfterSeasonEndDate(Product p) {
        if (LocalDate.now().plusDays(p.getLeadTime()).isAfter(p.getSeasonEndDate())) {
            notificationService.sendOutOfStockNotification(p.getName());
            p.setAvailable(Constants.zero);
            productRepository.save(p);
        }
    }

    private void notifyDelay(Product p){
        notifyDelay(p.getLeadTime(), p);
    }

    private void expireDateIsAfterToday(Product p){
        if (p.getAvailable() > Constants.zero && p.getExpiryDate().isAfter(LocalDate.now())) {
            p.setAvailable(p.getAvailable() - Constants.oneToAbstruct);
            productRepository.save(p);
        }
    }

    private void sendingExprationNotification(Product p){
        notificationService.sendExpirationNotification(p.getName(), p.getExpiryDate());
        p.setAvailable(Constants.zero);
        productRepository.save(p);
    }
}