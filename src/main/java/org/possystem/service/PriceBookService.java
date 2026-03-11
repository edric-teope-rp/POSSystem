package org.possystem.service;

import org.possystem.dao.PriceBookDao;
import org.possystem.entity.PriceBook;
import org.possystem.event.PosEvent;
import org.possystem.event.PosEventDispatcher;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Service class for PriceBook operations.
 * Implements PosEventDispatcher to fire events
 * when items are found or not found.
 */
public class PriceBookService implements PosEventDispatcher {

    private final PriceBookDao priceBookDao;

    public PriceBookService() {
        this.priceBookDao = new PriceBookDao();
    }

    public Optional<PriceBook> getItemByUpc(String upc) throws SQLException {
        Optional<PriceBook> item = priceBookDao.findByUpc(upc);
        if (item.isPresent()) {
            dispatchEvent(PosEvent.ITEM_ADDED, item.get());
        } else {
            dispatchEvent(PosEvent.ITEM_NOT_FOUND, upc);
        }
        return item;
    }

    public List<PriceBook> getAllItems() throws SQLException {
        return priceBookDao.findAll();
    }

    public List<PriceBook> getFeaturedItems() throws SQLException {
        return priceBookDao.findFeaturedItems();
    }

    public void updateProduct(String upc, String name, double price, boolean isFeatured) throws SQLException {
        priceBookDao.update(upc, name, price, isFeatured);
        dispatchEvent(PosEvent.ITEM_UPDATED, upc);
    }
}