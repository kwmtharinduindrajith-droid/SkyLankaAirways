package com.skylanka.airline.dto;

import com.skylanka.airline.enums.CabinClass;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class FlightSearchDto {
    private String originCode;
    private String destinationCode;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate travelDate;
    private CabinClass cabinClass = CabinClass.ECONOMY;
    private Double minPrice;
    private Double maxPrice;

    public FlightSearchDto() {}

    public String getOriginCode() { return originCode; }
    public void setOriginCode(String originCode) { this.originCode = originCode; }
    public String getDestinationCode() { return destinationCode; }
    public void setDestinationCode(String destinationCode) { this.destinationCode = destinationCode; }
    public LocalDate getTravelDate() { return travelDate; }
    public void setTravelDate(LocalDate travelDate) { this.travelDate = travelDate; }
    public CabinClass getCabinClass() { return cabinClass; }
    public void setCabinClass(CabinClass cabinClass) { this.cabinClass = cabinClass; }
    public Double getMinPrice() { return minPrice; }
    public void setMinPrice(Double minPrice) { this.minPrice = minPrice; }
    public Double getMaxPrice() { return maxPrice; }
    public void setMaxPrice(Double maxPrice) { this.maxPrice = maxPrice; }
}
