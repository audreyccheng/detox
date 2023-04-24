package shield.benchmarks.freehealth.models;

public class EpisodeContent {
    private Integer contentId;
    private Integer episodeId;
    private Integer contentType;
    private String xmlContent;
    private Integer prescriptionID;

    // TODO: currently treating prescription as nullable here, (want a variant for xmlContent OR prescription)
    public EpisodeContent(Integer episodeId, Integer contentType, String xmlContent, Integer prescriptionID) {
        this.episodeId = episodeId;
        this.contentType = contentType;
        this.xmlContent = xmlContent;
        this.prescriptionID = prescriptionID;
    }

    // in almost all cases, use the above and let the transaction select a contentID
    public EpisodeContent(Integer contentId, Integer episodeId, Integer contentType, String xmlContent, Integer prescriptionID) {
        this.contentId = contentId;
        this.episodeId = episodeId;
        this.contentType = contentType;
        this.xmlContent = xmlContent;
        this.prescriptionID = prescriptionID;
    }

    public Integer getContentId() {
        return contentId;
    }

    public void setContentId(Integer contentId) {
        this.contentId = contentId;
    }

    public Integer getEpisodeId() {
        return episodeId;
    }

    public void setEpisodeId(Integer episodeId) {
        this.episodeId = episodeId;
    }

    public Integer getContentType() {
        return contentType;
    }

    public void setContentType(Integer contentType) {
        this.contentType = contentType;
    }

    public String getXmlContent() {
        return xmlContent;
    }

    public void setXmlContent(String xmlContent) {
        this.xmlContent = xmlContent;
    }

    public Integer getPrescription() {
        return prescriptionID;
    }

    public void setPrescription(Integer prescriptionID) {
        this.prescriptionID = prescriptionID;
    }
}
