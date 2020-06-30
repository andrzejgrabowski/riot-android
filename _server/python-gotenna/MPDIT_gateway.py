# ~*~ coding: utf-8 ~*~
""" gateway.py: A starting point for running a gateway between the goTenna network and another external network.
"""

import logging
import sys
import datetime
import threading
import json
import asyncio
import socket
from gatewaystorage import GatewayStorage 

try:
    import goTenna
except ImportError:
    # Catch the case where this script is in a sibling directory of the SDK
    sys.path.append('..')
    import goTenna

# auto advetize period in minutes
AUTO_ADVERTIZE_PER_MIN = 5

_MODULE_LOGGER = logging.getLogger(__name__)


class Gateway:
    # pylint: enable=line-too-long
    """ A gateway module that lives halfway between a goTenna driver and an external communications module.

    In this example, the external communications module is just a CLI interface.
    """
    # pylint: disable=line-too-long
    _logger = _MODULE_LOGGER.getChild('Gateway')

    def _handle_message_to_forward(self, message):
        # pylint: disable=line-too-long
        """ This function is called when a message to forward is received.

        It should forward the message to the external network; here it simply prints the message.
        """
        #  pylint: enable=line-too-long
        if message.destination.gid_val not in self._storage.get(['external_contacts']).keys():
            self._logger.warning("External destination {} is not known"
                                 .format(message.destination))
        print("message to forward: {}".format(str(message)))
        # Since our "external endpoint" is really just printing the message,
        # we’ll send back an acknowledgement from here. In a real gateway system,
        # this acknowledgement message should be sent by whatever the external
        # recipient of the message is.


    def _handle_incoming_gotenna_message(self, evt):
        # All messages we receive serve to register the sender
        if evt.message.message_type == goTenna.constants.MESSAGE_TYPES['private']\
           and evt.message.payload.sender not in self._storage.get(['registered_gids']):
            self._storage.get(['registered_gids']).append(evt.message.payload.sender)
            self._logger.info("Registered GID {}".format(evt.message.payload.sender))
        if evt.message.destination.via_gateway == self._driver.gid:
            self._handle_message_to_forward(evt.message)
        else:
            self._logger.info("Incoming message: {}".format(evt.message))

    def _handle_connect(self, evt):
        if self._advertise_timer:
            self._logger.warning("Connected with advertise timer active - may be running multiple driver instances")
            self._advertise_timer.cancel()
            self._advertise_timer = None
        self.begin_auto_advertise(self._advertise_pacing)
        self._logger.info("Connected to device {}".format(evt.device_details))

    def _handle_disconnect(self, evt):
        self._logger.info("Device disconnected")
        self.end_auto_advertise()

    _EVENT_HANDLERS = {
        goTenna.driver.Event.MESSAGE: _handle_incoming_gotenna_message,
        goTenna.driver.Event.CONNECT: _handle_connect,
        goTenna.driver.Event.DISCONNECT: _handle_disconnect,
        goTenna.driver.Event.STATUS: None
    }

    def gotenna_event_callback(self, event):
        #print("next event from gotenna")
        handler = self._EVENT_HANDLERS.get(event.event_type,
                                           lambda s, e: s._logger.info(str(e)))
        self._logger.debug(event)
        if handler: handler(self, event)

    def __init__(self, gotenna_sdk_token,
                 advertise_pacing=datetime.timedelta(minutes=AUTO_ADVERTIZE_PER_MIN)):
        # pylint: disable=line-too-long
        """ Build the gateway.

        :param bytes gotenna_sdk_token: The token to pass to the goTenna driver instance.
        """
        settings_dict = None
        with open('setting_defaults.json') as f:
            settings_dict = json.load(f)
        
        rf_settings = None
        geo_settings = None
        spi_settings = None
        if 'pro' in settings_dict:
            if 'rf_settings' in settings_dict['pro']:
                rf_settings = goTenna.settings.RFSettings.from_dict(settings_dict['pro']['rf_settings'])
            if 'spi_settings' in settings_dict['pro']:
                spi_settings = goTenna.settings.SpiSettings.from_dict(settings_dict['pro']['spi_settings'])

        print(settings_dict)

        # pylint: enable=line-too-long
        self._storage = GatewayStorage(gotenna_sdk_token)
        if 'mesh' in settings_dict:
            if 'geo_settings' in settings_dict['mesh']:
                geo_settings = goTenna.settings.GeoSettings.from_dict(settings_dict['mesh']['geo_settings'])
        #settings=goTenna.settings.GoTennaSettings(rf_settings=rf_settings, geo_settings=geo_settings)
        settings = goTenna.settings.GoTennaSettings(rf_settings, geo_settings)
        self._driver = goTenna.driver.Driver(gotenna_sdk_token,
                                             goTenna.settings.GID.gateway(),
                                             settings, self.gotenna_event_callback,
                                             shortname='MPDIT',
                                             storage=self._storage, 
                                             do_automatic_connect=True)
        self._advertise_pacing = advertise_pacing
        self._advertise_timer = None
        self._driver.allow_automatic_connection(True)
        self._driver.start()

        
        accepted_device_types = self._driver.accepted_device_types
        print("accepted_device_types: ")
        print(accepted_device_types)

        blacklisted_devices = self._driver.blacklisted_devices
        print("blacklisted_devices: ")
        print(blacklisted_devices)

        can_connect = self._driver.can_connect
        print("Can connect: ")
        print(can_connect)

        connected = self._driver.connected
        print("Connected: ")
        print(connected)

        devices_present = self._driver.devices_present
        print("Devices present: ")
        print(devices_present)

        device_info = self._driver.device_info
        print("device_info: ")
        print(device_info)

        device_type = self._driver.device_type
        print("device_type: ")
        print(device_type)

        gid = self._driver.gid
        print("gid: ")
        print(gid)

        
        port = self._driver.port
        print("port: ")
        print(port)
        
        shortname = self._driver.shortname
        print("shortname: ")
        print(shortname)

        whitelisted_devices = self._driver.whitelisted_devices
        print("whitelisted_devices: ")
        print(whitelisted_devices)
        
        will_connect_automatically = self._driver.will_connect_automatically
        print("will_connect_automatically: ")
        print(will_connect_automatically)

        #PCIROOT(0)#PCI(1400)#USBROOT(0)#USB(5)
        #self._driver.connect("USB(5)")


        #self._driver.set_operation_mode(goTenna.constants.OperationModes.NORMAL)
        #self._driver.send_broadcast("Hello, world!")

    def begin_auto_advertise(self, pacing):
        # pylint: disable=line-too-long
        """ Begin automatic advertising at a given pacing.

        This should only be necessary to call if auto advertising is stopped manually or because of an error. In the normal course of events, it is called whenever a goTenna device connects to the gateway.

        To send an advertisement outside of this pace, use advertise().

        Note on implementation: This is implemented as nested functions so they can only be used in this context to prevent threading issues.
        """
        # pylint: enable=line-too-long
        def _auto_advertise():
            def _auto_ad_mcb(*args, **kwargs):
                if 'error' in kwargs:
                    self._logger.error("Error running auto advertise, stopping: {}"
                                       .format(kwargs.get('details', '<unknown>')))
                else:
                    # If the advertise call succeeded
                    self._logger.info("Auto-advertise sent")
                    if self._advertise_timer:
                        # Get rid of the timer if it’s still around
                        self._advertise_timer.join()
                    # Start a new timer
                    self._advertise_timer = threading.Timer(pacing.total_seconds(),
                                                            _auto_advertise)
                    self._advertise_timer.start()
            # Make the advertising call itself when the timer fires
            self.advertise(_auto_ad_mcb)
        # Start the first instance of the timer
        self._advertise_timer = threading.Timer(pacing.total_seconds(),
                                                _auto_advertise)
        self._advertise_timer.start()

    def end_auto_advertise(self):
        """ End automatic advertising.
        """
        if self._advertise_timer:
            self._advertise_timer.cancel()
            self._advertise_timer.join()

    def advertise(self, method_callback=None):
        # pylint: disable=line-too-long
        """ Send an advertisement on the goTenna network.

        The advertisement lists configured external contacts and their descriptions.
        """
        # pylint: enable=line-too-long
        advertisement_payload = goTenna.payload.GatewayAdvertisementPayload(
            [(goTenna.settings.GID(ec, goTenna.settings.GID.PRIVATE,
                                   goTenna.settings.GID.gateway()),
              ed)
             for ec, ed in self._storage.get(['external_contacts']).items()]
        )
        if None is method_callback:
            def ad_mcb(correlation_id, success=None, results=None,
                       error=None, details=None):
                if success:
                    self._logger.info("advertise sent")
                else:
                    self._logger.error("advertise failed: details={}"
                                       .format(details))
            method_callback = ad_mcb

        self._driver.send_broadcast(advertisement_payload, method_callback,
                                    is_repeated=True)

    def register_external(self, external_contact, description):
        # pylint: disable=line-too-long
        """ Register an external address.

        :param int external_contact: The contact.
        :param str description: A human-readable description for the contact, which will be sent along with the contact when advertising.
        """
        # pylint: enable=line-too-long
        self._storage.set({'external_contacts': {external_contact: description}})
        self._storage.store()

    def remove_external(self, external_contact):
        """ Remove an external contact, if it is registered.
        """
        try:
            extc = self._storage.get(['external_contacts'])
        except KeyError:
            pass
        extc.pop(external_contact)
        self._storage.store()

    def forward_broadcast(self, payload, from_external):
        # pylint: disable=line-too-long
        """ Forward a message from the external network to a broadcast on the goTenna network.

        The payload should be an already-constructed goTenna payload of the appropriate type, which means this method doesn't do much; it serves mostly as an example of how to configure a payload as received externally.
        """
        # pylint: enable=line-too-long
        payload.set_sender(from_external)
        def _sbmc(*args, **kwargs):
            if 'error'  in kwargs:
                self._logger.error("Could not forward broadcast: {}"
                                   .format(kwargs.get('details', '<unknown error>')))
            else:
                self._logger.info("Broadcast forwarded")
        self._driver.send_broadcast(payload, _sbmc)


    def register_dest_gid(self, dest_gid):
        # pylint: disable=line-too-long
        """ Register a destination gid.

        :param int external_contact: The contact.
        :param str description: A human-readable description for the contact, which will be sent along with the contact when advertising.
        """
        # pylint: enable=line-too-long
        self._storage.set({'registered_gids': {goTenna.settings.GID(int(dest_gid),
                                                                    goTenna.settings.GID.PRIVATE)}})
        self._storage.store()

    def remove_dest_gid(self, dest_gid):
        """ Remove a destination gid, if it is registered.
        """
        try:
            extc = self._storage.get(['registered_gids'])
        except KeyError:
            pass
        extc.pop(dest_gid)
        self._storage.store()

    def forward_private(self, payload, from_external, destination):
        # pylint: disable=line-too-long
        """ Forward a message from the external network to a private message on the goTenna network.

        The payload should be an already-constructed goTenna payload of the appropriate type, which means this method doesn't do much; it serves mostly as an example of how to configure a payload as received externally.
        """
        # pylint: enable=line-too-long
        payload.set_sender(from_external)
        if destination not in self._storage.get(['registered_gids']):
            self._logger.warning("Private message destination {} is not registered"
                                 .format(destination))
        def _spmc(*args, **kwargs):
            if 'error'  in kwargs:
                self._logger.error("Could not forward private message: {}"
                                   .format(kwargs.get('details', '<unknown error>')))
            else:
                self._logger.info("Private message forwarded")
        def _ack_callback(correlation_id, success):
            print("Ack for {}: {}".format(correlation_id,
                                      'ok!' if success else 'timed out!'))
        self._driver.send_private(destination,  payload, _spmc, _ack_callback, encrypt=False)

    def exit(self):
        self._handle_disconnect(None)
        sys.exit(1)
    
    def connection_made(self, transport):
        self.transport = transport
        print('connection made')

    def datagram_received(self, data, addr):
        message = data.decode()
        print('Received %r from %s' % (message, addr))
        print('Send %r to %s' % (message, addr))
        #self.transport.sendto(data, addr)






async def interact():
    """ Main interactable function called when executed as a script"""
    import argparse
    import six
    import code
    #parser = argparse.ArgumentParser('Run a dedicated goTenna mesh gateway')
    #parser.add_argument('SDK_TOKEN', type=six.b, help='The token for the goTenna SDK')
    #args = parser.parse_args()
    sdk_token = "RgIJCQMNEEQVQxlBUAsbHxwBQldHUlgDB0NSAxdRHx4LAwtZRFgLVw4DR1gcXgQE"
    gateway = Gateway(sdk_token)
    logging.basicConfig(level=logging.INFO)
    #code.interact(banner='goTenna Gateway', local={'g': g, 'goTenna': goTenna, 'd': g._driver})
    # w zamkniętej pętli przetwarzamy komunikaty UDP

    print("Starting UDP server")
    ## getting the hostname by socket.gethostname() method
    hostname = socket.gethostname()
    ## getting the IP address using socket.gethostbyname() method
    ip_address = socket.gethostbyname(hostname)
    ## printing the hostname and ip_address
    print(f"Hostname: {hostname}")
    print(f"IP Address: {ip_address}")

    # Get a reference to the event loop as we plan to use
    # low-level APIs.
    loop = asyncio.get_running_loop()

   
    # One protocol instance will be created to serve all
    # client requests.
    transport, protocol = await loop.create_datagram_endpoint(
        lambda: gateway,
        local_addr=(ip_address, 9999))

    try:
        await asyncio.sleep(3600)  # Serve for 1 hour.
    finally:
        transport.close()

if __name__ == '__main__':
    asyncio.run(interact())
    #interact()
