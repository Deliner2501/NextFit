package com.nextfit.service.face;

import com.nextfit.exceptions.ResourceNotFoundException;
import com.nextfit.model.Member;
import com.nextfit.service.MemberService;

import java.util.Optional;

/** Finds a member by its identifier. */
@FunctionalInterface
public interface MemberLookup {

    /**
     * @param id the identifier
     * @return the member, or empty when it does not exist
     */
    Optional<Member> find(long id);

    /**
     * @param service the members service
     * @return a lookup that asks the service
     */
    static MemberLookup of(MemberService service) {
        return id -> {
            try {
                return Optional.of(service.findById(id));
            } catch (ResourceNotFoundException e) {
                return Optional.empty();
            }
        };
    }
}
