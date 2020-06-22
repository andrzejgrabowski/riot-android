"""
This sampleDriver is intended for a direct interaction with the goTenna SDK, 
and uses a json file for the device configuration.

Usage: python sampleDriver -s settingFile.json [-m]

Copyright goTenna, Inc 2018
"""

import argparse
import code
import logging
import json
import os
import datetime
import sys
import uuid

import goTenna

logger = logging.getLogger(__name__)

def interact():
    # This part of the file makes it callable as a python script that runs an
    # interactive REPL with a driver available. To run the script
    # like this, do `python sampleDriver.py args` on a command line. The
    # available arguments (some of which are required) can be printed with
    # python sampleDriver.py --help
    #
    # Once the script starts, you can call methods of the driver directly
    # (the driver is available as `d`) and events such as incoming messages
    # will be printed to the console.
    parser = argparse.ArgumentParser(description='Interact with a goTenna device')
    def auto_int(x):
        return int(x, 0)
    parser.add_argument('-g', '--gid', type=auto_int,
                        help='Local GID. If unspecified, randomly generated')
    parser.add_argument('-s', '--settings', type=argparse.FileType('r'),
                        default=os.path.join(os.path.dirname(__file__),
                                            'setting_defaults.json'),
                        help='goTenna settings dict file to read. If unspecified, defaults are used')
    parser.add_argument('-m', '--manual-connect', action='store_true',
                        help='Do not automatically connect to devices')

    args = parser.parse_args()
    if args.gid:
        gid = goTenna.settings.GID(args.gid, goTenna.settings.GID.PRIVATE)
    else:
        gid = goTenna.settings.GID.generate(goTenna.settings.GID.PRIVATE)


    settings_dict = json.load(args.settings)
    logger.info(settings_dict)

    if 'sdk_token' in settings_dict:
        SDK_TOKEN = str(settings_dict['sdk_token'])
    else:
        logger.info('ERROR: missing SDK TOKEN in json config file !!!')
        sys.exit(1)

    rf_settings = None
    geo_settings = None
    spi_settings = None
    if 'pro' in settings_dict:
        if 'rf_settings' in settings_dict['pro']:
            rf_settings = goTenna.settings.RFSettings.from_dict(settings_dict['pro']['rf_settings'])
        if 'spi_settings' in settings_dict['pro']:
            spi_settings = goTenna.settings.SpiSettings.from_dict(settings_dict['pro']['spi_settings'])

    if 'mesh' in settings_dict:
        if 'geo_settings' in settings_dict['mesh']:
            geo_settings = goTenna.settings.GeoSettings.from_dict(settings_dict['mesh']['geo_settings'])

    settings=goTenna.settings.GoTennaSettings(rf_settings=rf_settings, geo_settings=geo_settings)
    if settings.rf_settings_valid:
        logger.info(settings.rf_settings)
    if settings.geo_settings_valid:
        logger.info(settings.geo_settings)
    if spi_settings != None:
        logger.info(spi_settings)

    messages = []

    def event_handler(evt):
        """ Default event handler that prints non-spammy messages"""
        if evt.event_type != evt.STATUS:
            logger.info(evt)
        if evt.event_type == evt.MESSAGE:
            messages.append(evt.message)

    def method_callback(*args, **kwargs):
        """ A default method callback for use with driver methods
        Example: d.send_broadcast(text_payload('hi'), method_callback)
        """
        if 'error' in kwargs:
            logger.info('Method call failed: {}'.format(kwargs['details']))
        else:
            logger.info('Method call succeeded!')

    def build_gid(val):
        """ A helper function for constructing a GID
        Example: d.send_private(build_gid(915213213213), text_payload('hi'), method_callback, ack_callback)
        """
        return goTenna.settings.GID(val, goTenna.settings.GID.PRIVATE)

    def ack_callback(correlation_id, success):
        """ A default acknowledge callback for use with d.send_private
        Example: d.send_private(build_gid(915213213213), text_payload('hi'), method_callback, ack_callback)
        """
        logger.info("Ack for {}: {}".format(correlation_id,
                                    'ok!' if success else 'timed out!'))

    def text_payload(message):
        """ Build a text payload easily to send in a message.
        Example: d.send_broadcast(text_payload('hi'), method_callback)
        """
        return goTenna.payload.TextPayload(message)

    if spi_settings == None:
        d = goTenna.driver.Driver(SDK_TOKEN, gid, settings, event_handler,
                            do_automatic_connect=not args.manual_connect)
        banner = 'goTenna USB testing harness.'
    else:
        d = goTenna.driver.SpiDriver(
                    spi_settings.bus_no, spi_settings.chip_no,
                    spi_settings.request_gpio, spi_settings.ready_gpio,
                    SDK_TOKEN, gid, settings, event_handler)
        banner = 'goTenna SPI testing harness.'
    d.start()
    code.interact(banner= banner + ' Driver is available as d',
                local={'d': d,
                        'method_callback': method_callback,
                        'goTenna': goTenna,
                        'build_gid': build_gid,
                        'ack_callback': ack_callback,
                        'messages': messages
                        })
    d.join()

def configLogger(filename):
    # create logger
    logger = logging.getLogger(name=None)
    logger.setLevel(logging.NOTSET)

    # create formatter
    formatter = logging.Formatter('[%(levelname)s:%(asctime)s:%(name)s] %(message)s')

    # create console handler and set level to info
    ch = logging.StreamHandler()
    ch.setLevel(logging.INFO)
    ch.setFormatter(formatter)
    logger.addHandler(ch)

    # create file handler and set level to debug
    fh = logging.FileHandler(filename, mode='w')
    fh.setLevel(logging.DEBUG)
    fh.setFormatter(formatter)
    logger.addHandler(fh)

if __name__ == '__main__':
    configLogger('goTennaSDK.log')
    interact()