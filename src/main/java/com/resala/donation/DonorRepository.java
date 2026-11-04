package com.resala.donation;

import com.resala.model.Donor;
import java.util.List;
import java.util.Optional;

public interface DonorRepository {
  Donor save(Donor donor);

  Optional<Donor> findById(long id);

  List<Donor> findActive(String query);

  void archive(long id);
}
