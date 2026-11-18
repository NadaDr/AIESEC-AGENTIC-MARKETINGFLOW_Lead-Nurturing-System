
package com.example.mcptoolsserver.service;

import com.example.mcptoolsserver.entities.Campaign;
import com.example.mcptoolsserver.repository.CampaignRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;

    public CampaignService(CampaignRepository campaignRepository) {

        this.campaignRepository = campaignRepository;
    }

    public Campaign saveCampaign(Campaign campaign) {

        return campaignRepository.save(campaign);
    }

    public List<Campaign> getAllCampaigns() {

        return campaignRepository.findAll();
    }

    public Optional<Campaign> getCampaignById(Long id) {

        return campaignRepository.findById(id);
    }

    public List<Campaign> getCampaignsByStatus(String status) {

        return campaignRepository.findByStatus(status);
    }

    public List<Campaign> getCampaignsByPlatform(String platform) {

        return campaignRepository.findByPlatform(platform);
    }

    public List<Campaign> getCampaignsByTargetAudience(String targetAudience) {

        return campaignRepository.findByTargetAudience(targetAudience);
    }

    public List<Campaign> searchCampaignsByName(String name) {

        return campaignRepository.findByNameContainingIgnoreCase(name);
    }

    public void deleteCampaign(Long id) {

        campaignRepository.deleteById(id);
    }
}
