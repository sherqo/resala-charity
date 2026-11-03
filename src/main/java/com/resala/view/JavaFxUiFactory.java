package com.resala.view;

import com.resala.controller.BeneficiaryController;
import com.resala.controller.DonationController;
import com.resala.controller.EventController;
import com.resala.controller.PaymentController;
import com.resala.controller.VolunteerController;
import com.resala.donation.DonationService;
import com.resala.event.EventService;

public class JavaFxUiFactory implements UiFactory {

    private final DonationController donationController;
    private final PaymentController paymentController;
    private final EventController eventController;
    private final VolunteerController volunteerController;
    private final DonationService donationService;
    private final EventService eventService;

    public JavaFxUiFactory(DonationController donationController,
                           PaymentController paymentController,
                           EventController eventController,
                           VolunteerController volunteerController,
                           DonationService donationService,
                           EventService eventService) {
        this.donationController = donationController;
        this.paymentController = paymentController;
        this.eventController = eventController;
        this.volunteerController = volunteerController;
        this.donationService = donationService;
        this.eventService = eventService;
    }

    @Override
    public DonationView createDonationView(DonationController controller, PaymentController payments) {
        // For JavaFX, the views are created in AppShell and managed there
        // This factory is kept for interface compatibility
        return new GuiDonationView(controller, payments);
    }

    @Override
    public EventView createEventView(EventController controller) {
        return new GuiEventView(controller);
    }

    @Override
    public VolunteerView createVolunteerView(VolunteerController controller) {
        return new GuiVolunteerView(controller);
    }

    @Override
    public BeneficiaryView createBeneficiaryView(BeneficiaryController controller) {
        return new GuiBeneficiaryView(controller);
    }

    // JavaFX-specific view wrappers that implement the console view interfaces
    // These are thin adapters - the real views are in AppShell

    private static class GuiDonationView implements DonationView {
        private final DonationController controller;
        private final PaymentController payments;

        GuiDonationView(DonationController controller, PaymentController payments) {
            this.controller = controller;
            this.payments = payments;
        }

        @Override
        public void start() {
            // Not used in JavaFX - views are managed by AppShell
        }
    }

    private static class GuiEventView implements EventView {
        private final EventController controller;

        GuiEventView(EventController controller) {
            this.controller = controller;
        }

        @Override
        public void start() {
            // Not used in JavaFX
        }
    }

    private static class GuiVolunteerView implements VolunteerView {
        private final VolunteerController controller;

        GuiVolunteerView(VolunteerController controller) {
            this.controller = controller;
        }

        @Override
        public void start() {
            // Not used in JavaFX
        }
    }

    private static class GuiBeneficiaryView implements BeneficiaryView {
        private final BeneficiaryController controller;

        GuiBeneficiaryView(BeneficiaryController controller) {
            this.controller = controller;
        }

        @Override
        public void start() {
            // Not used in JavaFX
        }
    }
}