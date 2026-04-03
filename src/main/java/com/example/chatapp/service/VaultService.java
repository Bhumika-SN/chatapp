package com.example.chatapp.service;

import com.example.chatapp.model.VaultItem;
import com.example.chatapp.repository.VaultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class VaultService {

    @Autowired private VaultRepository vaultRepository;

    public VaultItem save(VaultItem item) {
        return vaultRepository.save(item);
    }

    public List<VaultItem> getAll(String owner) {
        return vaultRepository.findByOwnerOrderByCreatedAtDesc(owner);
    }

    public List<VaultItem> getByType(String owner, String type) {
        return vaultRepository.findByOwnerAndTypeOrderByCreatedAtDesc(owner, type);
    }

    public Map<String, Object> delete(Long id, String owner) {
        Map<String, Object> res = new HashMap<>();
        vaultRepository.findById(id).ifPresentOrElse(item -> {
            if (item.getOwner().equals(owner)) {
                vaultRepository.delete(item);
                res.put("success", true);
            } else {
                res.put("error", "Not authorized");
            }
        }, () -> res.put("error", "Not found"));
        return res;
    }

    public Map<String, Object> toggleComplete(Long id, String owner) {
        Map<String, Object> res = new HashMap<>();
        vaultRepository.findById(id).ifPresentOrElse(item -> {
            item.setCompleted(!item.isCompleted());
            vaultRepository.save(item);
            res.put("success", true);
            res.put("completed", item.isCompleted());
        }, () -> res.put("error", "Not found"));
        return res;
    }
}