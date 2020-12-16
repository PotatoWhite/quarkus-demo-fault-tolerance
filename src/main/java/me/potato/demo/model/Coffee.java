package me.potato.demo.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Coffee {
  private Integer id;
  private String  name;
  private String  countryOfOrigin;
  private Integer price;
}
