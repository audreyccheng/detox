package shield.benchmarks.tpcc;

public abstract class DatabaseKey {

  /**
   * Returns a Integer version of the key (some form of hashing is used)
   */
  public abstract Long value();

  public String str() {
    return value().toString();
  }


};

class WarehouseKey extends DatabaseKey {

  private Long W_ID;

  WarehouseKey(Integer w_id) {
    W_ID = w_id.longValue();
  }

  @Override
  public Long value() {
    return W_ID;
  }
};

class DistrictKey extends DatabaseKey {

  private Long D_W_ID;
  private Long D_ID;

  DistrictKey(Integer d_w_id, Integer d_id) {
    D_W_ID = d_w_id.longValue();
    D_ID = d_id.longValue();
  }

  public Long value() {
    return (D_W_ID << 5) | D_ID;
  }
};

class CustomerKey extends DatabaseKey {

  private Long C_W_ID;
  private Long C_D_ID;
  private Long C_ID;

  CustomerKey(Integer c_w_id, Integer c_d_id, Integer c_id) {
    C_W_ID = c_w_id.longValue();
    C_D_ID = c_d_id.longValue();
    C_ID = c_id.longValue();
  }

  public Long value() {
    return (C_W_ID << 22) | (C_D_ID << 17) | C_ID;
  }
};

class HistoryKey extends DatabaseKey {

  private Long H_ID;

  HistoryKey(Integer h_id) {
    H_ID = h_id.longValue();
  }

  public Long value() {
    return H_ID;
  }
}

class NewOrderKey extends DatabaseKey {

  private Long NO_W_ID;
  private Long NO_D_ID;
  private Long NO_O_ID;

  NewOrderKey(Integer no_w_id, Integer no_d_id, Integer no_o_id) {
    NO_W_ID = no_w_id.longValue();
    NO_D_ID = no_d_id.longValue();
    NO_O_ID = no_o_id.longValue();
  }


  public Long value() {
    return (NO_W_ID << 29) | (NO_D_ID << 24) | NO_O_ID;
  }
};

class EarliestNewOrderKey extends DatabaseKey {

  private Long NO_W_ID;
  private Long NO_D_ID;

  EarliestNewOrderKey(Integer no_w_id, Integer no_d_id) {
    NO_W_ID = no_w_id.longValue();
    NO_D_ID = no_d_id.longValue();
  }

  public Long value() {
    return (NO_W_ID << 5) | NO_D_ID;
  }
};

class OrderKey extends DatabaseKey {

  private Long O_W_ID;
  private Long O_D_ID;
  private Long O_ID;

  OrderKey(Integer o_w_id, Integer o_d_id, Integer o_id) {
    O_W_ID = o_w_id.longValue();
    O_D_ID = o_d_id.longValue();
    O_ID = o_id.longValue();
  }

  public Long value() {
    return (O_W_ID << 29) | (O_D_ID << 24) | O_ID;
  }
};

// latest order from a particular user
class OrderByCustomerKey extends DatabaseKey {

  private Long O_W_ID;
  private Long O_D_ID;
  private Long O_C_ID;

  OrderByCustomerKey(Integer o_w_id, Integer o_d_id, Integer o_c_id) {
    O_W_ID = o_w_id.longValue();
    O_D_ID = o_d_id.longValue();
    O_C_ID = o_c_id.longValue();
  }

  public Long value() {
    return (O_W_ID << 22) | (O_D_ID << 17) | O_C_ID;
  }
};

// customer ids by name
class CustomerByNameKey extends DatabaseKey {

  private Long C_W_ID;
  private Long C_D_ID;
  private Long C_NAME;

  CustomerByNameKey(Integer c_w_id, Integer c_d_id, String c_name) {
    C_W_ID = c_w_id.longValue();
    C_D_ID = c_d_id.longValue();
    C_NAME = new Long(c_name.hashCode());
  }

  public Long value() {
    return (C_W_ID << 22) | (C_D_ID << 17) | C_NAME;
  }
};
class OrderLineKey extends DatabaseKey {

  private Long OL_W_ID;
  private Long OL_D_ID;
  private Long OL_O_ID;
  private Long OL_ID;

  OrderLineKey(Integer ol_w_id, Integer ol_d_id, Integer ol_o_id, Integer ol_id) {
    OL_W_ID = ol_w_id.longValue();
    OL_D_ID = ol_d_id.longValue();
    OL_O_ID = ol_o_id.longValue();
    OL_ID = ol_id.longValue();
  }

  public Long  value() {
    return (OL_W_ID << 33) | (OL_D_ID << 28) | (OL_O_ID << 4) | OL_ID;
  }
};

class ItemKey extends DatabaseKey {

  private Long I_ID;

  ItemKey(Integer i_id) {
    I_ID = i_id.longValue();
  }

  public Long value() {
    return I_ID;
  }
};

class StockKey extends DatabaseKey {

  private Long S_W_ID;
  private Long S_I_ID;

  StockKey(Integer s_w_id, Integer s_i_id) {
    S_W_ID =  s_w_id.longValue();
    S_I_ID =  s_i_id.longValue();
  }

  public Long value() {
    return (S_W_ID << 18) | S_I_ID;
  }

};